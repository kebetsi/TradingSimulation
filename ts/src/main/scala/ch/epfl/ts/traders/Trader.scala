package ch.epfl.ts.traders

import scala.language.postfixOps
import scala.reflect
import scala.reflect.ClassTag
import scala.concurrent.duration.DurationInt
import akka.actor.Props
import ch.epfl.ts.component.Component
import ch.epfl.ts.component.ComponentBuilder
import ch.epfl.ts.component.ComponentRef
import ch.epfl.ts.data.ParameterTrait
import ch.epfl.ts.data.StrategyParameters
import ch.epfl.ts.engine.GetWalletFunds
import ch.epfl.ts.engine.FundWallet
import ch.epfl.ts.data.Register
import ch.epfl.ts.data.Currency
import ch.epfl.ts.data.WalletParameter

case class RequiredParameterMissingException(message: String) extends RuntimeException(message)

/**
 * Abstract class to be extended by all concrete Trader implementations.
 * A Trader represents the implementation of a trading strategy.
 * 
 * It factorizes parameter handling for concrete trading strategies.
 * 
 * It will throw a `RequiredParameterMissingException` on instantiation if any of the
 * required parameters have not been provided (or have the wrong type).
 */
abstract class Trader(val uid: Long, val parameters: StrategyParameters) extends Component {
  /** Gives a handle to the companion object */
  def companion: TraderCompanion
  
  // On instantiation, check that all mandatory parameters have been provided
  companion.verifyParameters(parameters)
  // TODO: warn if some parameters are unused
  
  /** Default timeout to use when Asking another component asynchronously */
  val askTimeout = 500 milliseconds
  
  val initialFunds = parameters.get[Map[Currency.Currency, Double]]("InitialFunds")
  
  /**
   * Initialization common to all trading strategies:
   *   - Register to the broker
   *   - Put initial funds into our wallet
   */
  final override def start = {
    
    println("Common start function received")
    
    // Register with the broker we are connected to
    send(Register(uid))
    // Fund the wallet using the provided currency
    for {
      (currency, funds) <- initialFunds
      if funds > 0.0
    } yield {
      send(FundWallet(uid, currency, funds))
    }
    send(GetWalletFunds(uid,this.self))
    
    // Strategy-specific initialization
    init
  }
  
  /**
   * Strategies wishing to perform some initialization may
   * override this function.
   */
  def init = {}
}

/**
 * Trait to be extended by the Trader companion objects.
 * In order for the strategy to be automatically testable, the companion object
 * declares the stragey's parameters (required and optional).
 * 
 * The user should not have to specify the parameter names as strings, but rather
 * be able to use the keys exposed by the strategy's companion object.
 */
trait TraderCompanion {
  type Key = String
  
  /**
   * Abstract type which needs to be set by companions to the
   * concrete Trader class they accompany
   */
  type ConcreteTrader <: Trader
  /**
   * Needed to overcome type erasure.
   * @example `override protected val concreteTraderTag = scala.reflect.classTag[MadTrader]`
   */
  protected implicit def concreteTraderTag: ClassTag[ConcreteTrader]
  
  /**
   * Check that the given parameters are valid with respect to what is declared by this strategy,
   * and create a new parameterized Trader if it is the case.
   * Otherwise, throw a an exception.
   * 
   * This is the preferred method to instantiate a Trader, as it will perform parameter checking first. 
   */
  final def getInstance(uid: Long, parameters: StrategyParameters, name: String)(implicit builder: ComponentBuilder): ComponentRef = {
    verifyParameters(parameters)
    getConcreteInstance(builder, uid, parameters, name)
  }
  
  /**
   * Provide a new instance of the concrete trading strategy using these parameters.
   * Can be overriden by concrete TraderCompanion, but the generic implementation should be sufficient.
   */
  protected def getConcreteInstance(builder: ComponentBuilder,
                                    uid: Long, parameters: StrategyParameters,
                                    name: String) = {
    builder.createRef(Props(concreteTraderTag.runtimeClass, uid, parameters), name)
  }
  
  /**
   * Initial funds: represents the initial state of the wallet (can
   * contain several currencies).
   * This is a parameter required for all trading strategies
   */
  val INITIAL_FUNDS = "InitialFunds"
  
  
  /**
   * All parameters required to instantiate this strategy.
   */
  final def requiredParameters = Map[Key, ParameterTrait](
    INITIAL_FUNDS -> WalletParameter
  ) ++ strategyRequiredParameters
  
  /**
   * Parameters for which the user of the strategy *must* provide a value.
   */
  protected def strategyRequiredParameters: Map[Key, ParameterTrait]
  
  /**
   * If the user of the strategy doesn't provide
   * Should not overlap with requiredParameters
   */
  def optionalParameters: Map[Key, ParameterTrait] = Map()
  
  /**
   * All (required and optional) parameters
   */
  def parameters: Map[Key, ParameterTrait] = requiredParameters ++ optionalParameters
  
  /**
   * Verify that all required parameters (as specified by this TraderCompanion)
   * have been provided in `parameters`.
   * Otherwise, throw an exception.
   */
  def verifyParameters(parameters: StrategyParameters) = for {
    p <- requiredParameters
    key = p._1
    theType = p._2
    if !parameters.hasWithType(key, theType)
  } yield throw new RequiredParameterMissingException("Trading strategy requires parameter " + key + " with type " + theType)
}
