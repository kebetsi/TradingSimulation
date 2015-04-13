package ch.epfl.ts.traders

import ch.epfl.ts.component.Component
import ch.epfl.ts.data.ParameterTrait

/**
 * Abstract class to be extended by all concrete Trader implementations.
 * A Trader represents the implementation of a trading strategy.
 */
abstract class Trader extends Component {
  
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
  def optionalParameters: Map[Key, ParameterTrait[_]]
  
  def parameters: Map[Key, ParameterTrait[_]] = requiredParameters ++ optionalParameters
}