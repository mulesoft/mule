package org.mule.api.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;

public class LoggerMessageProcessorTestCase 
{

    @Test
    public void logNullEvent()
    {
        // Test with level enabled
        LoggerMessageProcessor loggerMessageProcessor = buildLoggerMessageProcessorWithLevel("ERROR");
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger).error(null);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isErrorEnabled()).thenReturn(false);
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger, never()).error(null); 
        
        // Test with level enabled
        loggerMessageProcessor.setLevel("WARN");
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger).warn(null);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isWarnEnabled()).thenReturn(false);
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger, never()).warn(null); 

        // Test with level enabled
        loggerMessageProcessor.setLevel("INFO");
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger).info(null);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isInfoEnabled()).thenReturn(false);
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger, never()).info(null); 

        // Test with level enabled
        loggerMessageProcessor.setLevel("DEBUG");
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger).debug(null);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isDebugEnabled()).thenReturn(false);
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger, never()).debug(null);
        
        // Test with level enabled
        loggerMessageProcessor.setLevel("TRACE");
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger).trace(null);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isTraceEnabled()).thenReturn(false);
        loggerMessageProcessor.log(null);
        verify(loggerMessageProcessor.logger, never()).trace(null);
    }
    
    @Test
    public void logMuleMessage()
    {
        MuleEvent muleEvent = buildMuleEvent();

        // Test with level enabled
        LoggerMessageProcessor loggerMessageProcessor = buildLoggerMessageProcessorWithLevel("ERROR");
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger).error(muleEvent.getMessage());
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isErrorEnabled()).thenReturn(false);
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).error(muleEvent.getMessage()); 
        
        // Test with level enabled
        loggerMessageProcessor.setLevel("WARN");
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger).warn(muleEvent.getMessage());
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isWarnEnabled()).thenReturn(false);
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).warn(muleEvent.getMessage()); 

        // Test with level enabled
        loggerMessageProcessor.setLevel("INFO");
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger).info(muleEvent.getMessage());
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isInfoEnabled()).thenReturn(false);
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).info(muleEvent.getMessage()); 

        // Test with level enabled
        loggerMessageProcessor.setLevel("DEBUG");
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger).debug(muleEvent.getMessage());
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isDebugEnabled()).thenReturn(false);
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).debug(muleEvent.getMessage());
        
        // Test with level enabled
        loggerMessageProcessor.setLevel("TRACE");
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger).trace(muleEvent.getMessage());
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isTraceEnabled()).thenReturn(false);
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).trace(muleEvent.getMessage());
    }
    
    @Test
    public void logMessage()
    {
        MuleEvent muleEvent = buildMuleEvent();
        
        // Test with level enabled
        LoggerMessageProcessor loggerMessageProcessor = buildLoggerMessageProcessorWithLevel("ERROR");
        loggerMessageProcessor.setMessage("some expression");
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, times(1)).error("text to log");
        verify(loggerMessageProcessor.expressionManager, times(1)).parse("some expression", muleEvent);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isErrorEnabled()).thenReturn(false);
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).error("text to log");
        verify(loggerMessageProcessor.expressionManager, never()).parse("some expression", muleEvent);

        // Test with level enabled
        loggerMessageProcessor.setLevel("WARN");
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, times(1)).warn("text to log");
        verify(loggerMessageProcessor.expressionManager, times(1)).parse("some expression", muleEvent);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isWarnEnabled()).thenReturn(false);
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).warn("text to log");
        verify(loggerMessageProcessor.expressionManager, never()).parse("some expression", muleEvent);
        
        // Test with level enabled
        loggerMessageProcessor.setLevel("INFO");
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, times(1)).info("text to log");
        verify(loggerMessageProcessor.expressionManager, times(1)).parse("some expression", muleEvent);
        // Test with level disabled
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isInfoEnabled()).thenReturn(false);
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).info("text to log");
        verify(loggerMessageProcessor.expressionManager, never()).parse("some expression", muleEvent);

        loggerMessageProcessor.setLevel("DEBUG");
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, times(1)).debug("text to log");
        verify(loggerMessageProcessor.expressionManager, times(1)).parse("some expression", muleEvent);
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isDebugEnabled()).thenReturn(false);
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).debug("text to log");
        verify(loggerMessageProcessor.expressionManager, never()).parse("some expression", muleEvent);

        loggerMessageProcessor.setLevel("TRACE");
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, times(1)).trace("text to log");
        verify(loggerMessageProcessor.expressionManager, times(1)).parse("some expression", muleEvent);
        loggerMessageProcessor.logger = buildMockLogger();
        when(loggerMessageProcessor.logger.isTraceEnabled()).thenReturn(false);
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, never()).trace("text to log");
        verify(loggerMessageProcessor.expressionManager, never()).parse("some expression", muleEvent);
    }
    
    private Log buildMockLogger()
    {
        Log mockLogger = mock(Log.class);
        doNothing().when(mockLogger).error(any());
        doNothing().when(mockLogger).warn(any());
        doNothing().when(mockLogger).info(any());
        doNothing().when(mockLogger).debug(any());
        doNothing().when(mockLogger).trace(any());
        
        // All levels enabled by default
        when(mockLogger.isErrorEnabled()).thenReturn(true);
        when(mockLogger.isWarnEnabled()).thenReturn(true);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(mockLogger.isTraceEnabled()).thenReturn(true);        
        return mockLogger;
    }
    
    private LoggerMessageProcessor buildLoggerMessageProcessorWithLevel(String level)
    {
        LoggerMessageProcessor loggerMessageProcessor = new LoggerMessageProcessor();
        loggerMessageProcessor.initLogger();
        loggerMessageProcessor.logger = buildMockLogger();
        loggerMessageProcessor.setLevel(level);
        return loggerMessageProcessor;
    }
    
    private MuleEvent buildMuleEvent()
    {
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage message = mock(MuleMessage.class);
        when(message.toString()).thenReturn("text to log");
        when(event.getMessage()).thenReturn(message );
        return event;
    }
    
    private ExpressionManager buildExpressionManager()
    {
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        when(expressionManager.parse(anyString(), any(MuleEvent.class))).thenReturn("text to log");
        return expressionManager;
    }

}