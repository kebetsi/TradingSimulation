package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.ParameterTrait
import ch.epfl.ts.data.StrategyParameters

/**
 * Abstract class to be extended by all concrete Trader implementations.
 * A Trader represents the implementation of a trading strategy.
 * 
 * It factorizes parameter handling for concrete trading strategies.
 */
abstract class Trader(parameters: StrategyParameters) extends Component {
  /** Gives a handle to the companion object */
  def companion: TraderCompanion
  
  /**
   * On instantiation, check that all mandatory parameters have been provided
   */
  for {
    p <- companion.requiredParameters
    key = p._1
    theType = p._2
  } yield assert(parameters.hasWithType(key, theType), "Trading strategy requires parameter " + key + " with type " + theType)
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
   * Parameters for which the user of the strategy *must* provide a value.
   */
  def requiredParameters: Map[Key, ParameterTrait[_]]
  
  /**
   * If the user of the strategy doesn't provide
   * Should not overlap with requiredParameters
   */
  def optionalParameters: Map[Key, ParameterTrait[_]] = Map()
  
  def parameters: Map[Key, ParameterTrait[_]] = requiredParameters ++ optionalParameters
}
