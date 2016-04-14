/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.verification.VerificationMode;

public class LoggerMessageProcessorTestCase extends AbstractMuleTestCase 
{
    
    @Test  
    public void logNullEvent()
    {
        verifyNullEventByLevel("TRACE");
        verifyNullEventByLevel("DEBUG");
        verifyNullEventByLevel("INFO");
        verifyNullEventByLevel("WARN");
        verifyNullEventByLevel("ERROR");
    }
    
    @Test
    public void logMuleEvent()
    {
        verifyMuleEventByLevel("TRACE");
        verifyMuleEventByLevel("DEBUG");
        verifyMuleEventByLevel("INFO");
        verifyMuleEventByLevel("WARN");
        verifyMuleEventByLevel("ERROR");
    }
    
    @Test
    public void logWithMessage()
    {
        verifyLoggerMessageByLevel("TRACE");
        verifyLoggerMessageByLevel("DEBUG");
        verifyLoggerMessageByLevel("INFO");
        verifyLoggerMessageByLevel("WARN");
        verifyLoggerMessageByLevel("ERROR");
    }    
    
    // Verifies if the right call to the logger was made depending on the level enabled
    private void verifyLogCall(LoggerMessageProcessor loggerMessageProcessor, String logLevel, String enabledLevel, MuleEvent muleEvent, Object objectToLog)
    {
        when(loggerMessageProcessor.logger.isTraceEnabled()).thenReturn("TRACE".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isDebugEnabled()).thenReturn("DEBUG".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isInfoEnabled()).thenReturn("INFO".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isWarnEnabled()).thenReturn("WARN".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isErrorEnabled()).thenReturn("ERROR".equals(enabledLevel));
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.logger, times("TRACE".equals(enabledLevel) ? 1 : 0)).trace(objectToLog);
        verify(loggerMessageProcessor.logger, times("DEBUG".equals(enabledLevel) ? 1 : 0)).debug(objectToLog);
        verify(loggerMessageProcessor.logger, times("INFO".equals(enabledLevel) ? 1 : 0)).info(objectToLog);
        verify(loggerMessageProcessor.logger, times("WARN".equals(enabledLevel) ? 1 : 0)).warn(objectToLog);
        verify(loggerMessageProcessor.logger, times("ERROR".equals(enabledLevel) ? 1 : 0)).error(objectToLog);
    }
    
    // Verifies if the Mule expression is called or not depending on the logging level enabled
    private void verifyExpressionEvaluation(LoggerMessageProcessor loggerMessageProcessor, String level, String enabledLevel, MuleEvent muleEvent, VerificationMode timesEvaluateExpression)
    {
        when(loggerMessageProcessor.logger.isTraceEnabled()).thenReturn("TRACE".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isDebugEnabled()).thenReturn("DEBUG".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isInfoEnabled()).thenReturn("INFO".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isWarnEnabled()).thenReturn("WARN".equals(enabledLevel));
        when(loggerMessageProcessor.logger.isErrorEnabled()).thenReturn("ERROR".equals(enabledLevel));        
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.log(muleEvent);
        verify(loggerMessageProcessor.expressionManager, timesEvaluateExpression).parse("some expression", muleEvent);
    }
    
    // Orchestrates the verifications for a call with a null MuleEvent
    private void verifyNullEventByLevel(String level)
    {
        LoggerMessageProcessor loggerMessageProcessor = buildLoggerMessageProcessorWithLevel(level);
        verifyLogCall(loggerMessageProcessor, level, level, null, null); // Level is enabled
        loggerMessageProcessor = buildLoggerMessageProcessorWithLevel(level);
        verifyLogCall(loggerMessageProcessor, level, "not" + level, null, null); // Level is disabled by prepending it with "not"
    }

    // Orchestrates the verifications for a call with a MuleEvent
    private void verifyMuleEventByLevel(String level)
    {
        LoggerMessageProcessor loggerMessageProcessor = buildLoggerMessageProcessorWithLevel(level);
        MuleEvent muleEvent = buildMuleEvent();
        verifyLogCall(loggerMessageProcessor, level, level, muleEvent, muleEvent.getMessage()); // Level is enabled
        loggerMessageProcessor = buildLoggerMessageProcessorWithLevel(level);
        verifyLogCall(loggerMessageProcessor, level, "not" + level, muleEvent, muleEvent.getMessage()); // Level is disabled by prepending it with "not"
    }

    // Orchestrates the verifications for a call with a 'message' set on the logger
    private void verifyLoggerMessageByLevel(String level)
    {
        MuleEvent muleEvent = buildMuleEvent();
        verifyLogCall(buildLoggerMessageProcessorForExpressionEvaluation(level), level, level, muleEvent, "text to log"); // Level is enabled
        verifyLogCall(buildLoggerMessageProcessorForExpressionEvaluation(level), level, "not" + level, muleEvent,"text to log"); // Level is disabled by prepending it with "not"
        verifyExpressionEvaluation(buildLoggerMessageProcessorForExpressionEvaluation(level), level, level, muleEvent, times(1)); // Expression should be evaluated when the level is enabled
        verifyExpressionEvaluation(buildLoggerMessageProcessorForExpressionEvaluation(level), level, "not"+ level, muleEvent, never()); // Expression should not be evaluated when the level is enabled
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
    
    private LoggerMessageProcessor buildLoggerMessageProcessorForExpressionEvaluation(String level)
    {
        LoggerMessageProcessor loggerMessageProcessor = buildLoggerMessageProcessorWithLevel(level);
        loggerMessageProcessor = buildLoggerMessageProcessorWithLevel(level);
        loggerMessageProcessor.expressionManager = buildExpressionManager();
        loggerMessageProcessor.setMessage("some expression");        
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