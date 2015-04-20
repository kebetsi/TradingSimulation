package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.ParameterTrait
import ch.epfl.ts.data.StrategyParameters

case class RequiredParameterMissingException(message: String) extends RuntimeException(message)

/**
 * Abstract class to be extended by all concrete Trader implementations.
 * A Trader represents the implementation of a trading strategy.
 * 
 * It factorizes parameter handling for concrete trading strategies.
 * 
 * It will throw a `RequiredParmaeterMissingException` on instantiation if any of the
 * required parameters have not been provided (or have the wrong type).
 */
abstract class Trader(parameters: StrategyParameters) extends Component {
  /** Gives a handle to the companion object */
  def companion: TraderCompanion
  
  // On instantiation, check that all mandatory parameters have been provided
  companion.verifyParameters(parameters)
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
   * Check that the given parameters are valid with respect to what is declared by this strategy,
   * and create a new parameterized Trader if it is the case.
   * Otherwise, throw a an exception.
   * 
   * This is the preferred method to instantiate a Trader, as it will perform parameter checking first. 
   */
  final def getInstance(uid: Long, parameters: StrategyParameters): Trader = {
    verifyParameters(parameters)
    getConcreteInstance(uid, parameters)
  }
  
  /**
   * Provide a new instance of the concrete trading strategy using these parameters.
   * To be overriden for each concrete TraderCompanion.
   */
  // TODO: need to use the implicit actor system, or the ComponentBuilder or something
  protected def getConcreteInstance(uid: Long, parameters: StrategyParameters): Trader
  
  /**
   * Parameters for which the user of the strategy *must* provide a value.
   */
  def requiredParameters: Map[Key, ParameterTrait]
  
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
    if(!parameters.hasWithType(key, theType))
  } yield throw new RequiredParameterMissingException("Trading strategy requires parameter " + key + " with type " + theType)
}
