/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import org.glassfish.grizzly.attributes.AttributeHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.slf4j.LoggerFactory;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mule.module.http.internal.HttpMessageLogger.LoggerType.LISTENER;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class, HttpMessageLogger.class})
public class HttpMessageLoggerTestCase {


    private Connection connection;
    private Buffer buffer;
    private AttributeHolder attributeHolder;
    private ClassLoader classLoader;
    private Logger logger;
    private HttpMessageLogger httpMessageLogger;

    @Before
    public void setup()
    {
        mockStatic(LoggerFactory.class);
        logger = mock(Logger.class);
        connection = mock(Connection.class);
        buffer = mock(Buffer.class);
        attributeHolder = mock(AttributeHolder.class);

        when(LoggerFactory.getLogger(HttpMessageLogger.class)).thenReturn(logger);
        doNothing().when(logger).debug(anyString());
        when(connection.getAttributes()).thenReturn(attributeHolder);

    }

    @Test
    public void loggerWithClassLoader() throws Exception
    {
        classLoader = mock(ClassLoader.class);
        when(attributeHolder.getAttribute(anyString())).thenReturn(classLoader);
        when(logger.isDebugEnabled()).thenReturn(true);

        httpMessageLogger = spy(new HttpMessageLogger(LISTENER));
        httpMessageLogger.onDataSentEvent(connection, buffer);

        verifyPrivate(httpMessageLogger, times(2))
                .invoke("setContextClassLoader", anyObject(), anyObject(), anyObject());
    }

    @Test
    public void loggerWithNullClassLoader() throws Exception
    {
        when(attributeHolder.getAttribute(anyString())).thenReturn(null);
        when(logger.isDebugEnabled()).thenReturn(true);

        httpMessageLogger = spy(new HttpMessageLogger(LISTENER));
        httpMessageLogger.onDataSentEvent(connection, buffer);

        verifyPrivate(httpMessageLogger, times(0))
                .invoke("setContextClassLoader", anyObject(), anyObject(), anyObject());
    }

    @Test
    public void loggerThrowException() throws Exception
    {
        classLoader = mock(ClassLoader.class);
        when(attributeHolder.getAttribute(anyString())).thenReturn(classLoader);
        when(logger.isDebugEnabled()).thenThrow(new NullPointerException());

        httpMessageLogger = spy(new HttpMessageLogger(LISTENER));
        try {
            httpMessageLogger.onDataSentEvent(connection, buffer);
        }catch (Exception ignored) {}
        verifyPrivate(httpMessageLogger, times(2))
                .invoke("setContextClassLoader", anyObject(), anyObject(), anyObject());
    }
}
