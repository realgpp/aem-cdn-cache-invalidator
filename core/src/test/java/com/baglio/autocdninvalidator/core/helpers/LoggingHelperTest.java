package com.baglio.autocdninvalidator.core.helpers;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baglio.autocdninvalidator.core.service.CdnInvalidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith({MockitoExtension.class})
class LoggingHelperTest {

  private LoggingHelper loggingHelper;
  @Mock private Logger logger;
  MockedStatic<LoggerFactory> loggerFactoryMockedStatic = mockStatic(LoggerFactory.class);

  @BeforeEach
  void setUp() {
    loggerFactoryMockedStatic.when(() -> LoggerFactory.getLogger(CdnInvalidationService.class)).thenReturn(logger);
    // create a logging helper instance for testing
    loggingHelper = new LoggingHelper(CdnInvalidationService.class);
  }

  @AfterEach
  void tearDown() {
    loggerFactoryMockedStatic.close();
  }

  @Test
  void debug_shouldLogMessage_whenDebugEnabled() {
    // given
    String message = "This is a debug message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return true for isDebugEnabled()
    when(logger.isDebugEnabled()).thenReturn(true);

    // when
    loggingHelper.debug(message, args);

    // then
    // verify that the logger was called with the same message and args
    verify(logger).debug(message, args);
    verify(logger, times(1)).debug(message, args);
  }

  @Test
  void debug_shouldNotLogMessage_whenDebugDisabled() {
    // given
    String message = "This is a debug message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return false for isDebugEnabled()
    when(logger.isDebugEnabled()).thenReturn(false);

    // when
    loggingHelper.debug(message, args);

    // then
    // verify that the logger was not called
    verify(logger).isDebugEnabled();
    verify(logger, times(0)).debug(message, args);
  }

  @Test
  void info_shouldLogMessage_whenInfoEnabled() {
    // given
    String message = "This is an info message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return true for isInfoEnabled()
    when(logger.isInfoEnabled()).thenReturn(true);

    // when
    loggingHelper.info(message, args);

    // then
    // verify that the logger was called with the same message and args
    verify(logger).info(message, args);
    verify(logger, times(1)).info(message, args);
  }

  @Test
  void info_shouldNotLogMessage_whenInfoDisabled() {
    // given
    String message = "This is an info message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return false for isInfoEnabled()
    when(logger.isInfoEnabled()).thenReturn(false);

    // when
    loggingHelper.info(message, args);

    // then
    // verify that the logger was not called
    verify(logger).isInfoEnabled();
    verify(logger, times(0)).info(message, args);
  }

  @Test
  void warn_shouldLogMessage_whenWarnEnabled() {
    // given
    String message = "This is a warn message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return true for isWarnEnabled()
    when(logger.isWarnEnabled()).thenReturn(true);

    // when
    loggingHelper.warn(message, args);

    // then
    // verify that the logger was called with the same message and args
    verify(logger).warn(message, args);
    verify(logger, times(1)).warn(message, args);
  }

  @Test
  void warn_shouldNotLogMessage_whenWarnDisabled() {
    // given
    String message = "This is a warn message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return false for isWarnEnabled()
    when(logger.isWarnEnabled()).thenReturn(false);

    // when
    loggingHelper.warn(message, args);

    // then
    // verify that the logger was not called
    verify(logger).isWarnEnabled();
    verify(logger, times(0)).warn(message, args);
  }

  @Test
  void trace_shouldLogMessage_whenTraceEnabled() {
    // given
    String message = "This is a trace message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return true for isTraceEnabled()
    when(logger.isTraceEnabled()).thenReturn(true);

    // when
    loggingHelper.trace(message, args);

    // then
    // verify that the logger was called with the same message and args
    verify(logger).trace(message, args);
    verify(logger, times(1)).trace(message, args);
  }

  @Test
  void trace_shouldNotLogMessage_whenTraceDisabled() {
    // given
    String message = "This is a trace message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return false for isTraceEnabled()
    when(logger.isTraceEnabled()).thenReturn(false);

    // when
    loggingHelper.trace(message, args);

    // then
    // verify that the logger was not called
    verify(logger).isTraceEnabled();
    verify(logger, times(0)).trace(message, args);
  }

  @Test
  void error_shouldLogMessage_whenErrorEnabled() {
    // given
    String message = "This is an error message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return true for isErrorEnabled()
    when(logger.isErrorEnabled()).thenReturn(true);

    // when
    loggingHelper.error(message, args);

    // then
    // verify that the logger was called with the same message and args
    verify(logger).error(message, args);
    verify(logger, times(1)).error(message, args);
  }

  @Test
  void error_shouldNotLogMessage_whenErrorDisabled() {
    // given
    String message = "This is an error message";
    Object[] args = new Object[] {"arg1", "arg2"};
    // mock the logger to return false for isErrorEnabled()
    when(logger.isErrorEnabled()).thenReturn(false);

    // when
    loggingHelper.error(message, args);

    // then
    // verify that the logger was not called
    verify(logger).isErrorEnabled();
    verify(logger, times(0)).error(message, args);
  }
}
