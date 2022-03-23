package com.coolerfall.download

/**
 * A simple indirection for logging debug messages.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
fun interface Logger {

  companion object {
    internal val EMPTY: Logger = Logger { }
  }

  /**
   * Output log with given message.
   *
   * @param message message
   */
  fun log(message: String)
}