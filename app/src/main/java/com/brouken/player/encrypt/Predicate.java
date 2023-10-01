package com.brouken.player.encrypt;

/**
 * Determines a true or false value for a given input.
 *
 * @param <T> The input type of the predicate.
 */
public interface Predicate<T> {

  /**
   * Evaluates an input.
   *
   * @param input The input to evaluate.
   * @return The evaluated result.
   */
  boolean evaluate(T input);

}
