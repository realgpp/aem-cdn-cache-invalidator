package com.baglio.autocdninvalidator.core.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to perform conditional logging based on log levels. Encapsulates log level checks and handles creating
 * class-specific logger.
 */
public class LoggingHelper {

  private final Logger logger;

  /**
   * Construct helper for the given class to log messages to. This will create a Logger named after the class.
   *
   * @param clazz the class to name the logger after
   */
  public LoggingHelper(final Class clazz) {
    this.logger = LoggerFactory.getLogger(clazz);
  }

  /**
   * Log a debug message if debug logging is enabled.
   *
   * @param message the log message possibly with {} placeholders
   * @param args arguments to fill placeholders
   */
  public void debug(final String message, final Object... args) {
    if (logger.isDebugEnabled()) {
      logger.debug(message, args);
    }
  }

  /**
   * Log an info message if info logging is enabled.
   *
   * @param message the log message possibly with {} placeholders
   * @param args arguments to fill placeholders
   */
  public void info(final String message, final Object... args) {
    if (logger.isInfoEnabled()) {
      logger.info(message, args);
    }
  }

  /**
   * Log a warn message if warn logging is enabled.
   *
   * @param message the log message possibly with {} placeholders
   * @param args arguments to fill placeholders
   */
  public void warn(final String message, final Object... args) {
    if (logger.isWarnEnabled()) {
      logger.warn(message, args);
    }
  }

  /**
   * Log a trace message if trace logging is enabled.
   *
   * @param message the log message possibly with {} placeholders
   * @param args arguments to fill placeholders
   */
  public void trace(final String message, final Object... args) {
    if (logger.isTraceEnabled()) {
      logger.trace(message, args);
    }
  }

  /**
   * Log an error message if error logging is enabled.
   *
   * @param message the log message possibly with {} placeholders
   * @param args arguments to fill placeholders
   */
  public void error(final String message, final Object... args) {
    if (logger.isErrorEnabled()) {
      logger.error(message, args);
    }
  }
}
