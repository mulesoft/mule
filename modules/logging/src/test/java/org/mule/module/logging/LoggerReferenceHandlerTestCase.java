/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.logging;

import java.lang.ref.ReferenceQueue;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class LoggerReferenceHandlerTestCase
{

    private ReferenceQueue referenceQueue;
    private Map references;
    private Map loggerRepository;

    @Before
    public void setupp()
    {
        referenceQueue = mock(ReferenceQueue.class);
        references = mock(Map.class);
        loggerRepository = mock(Map.class);
    }

    @Test
    public void testLoggerReferenceHandler()
    {
        LoggerReferenceHandler lrh = new LoggerReferenceHandler("thread", referenceQueue, references, loggerRepository);
        assertNotNull(lrh);
    }

    @Test
    public void testLoggerReferenceHandlerInterruptedException() throws Exception
    {
        stub(referenceQueue.remove()).toThrow(new InterruptedException("mock exception"));
        LoggerReferenceHandler lrh = new LoggerReferenceHandler("thread", referenceQueue, references, loggerRepository);
        assertNotNull(lrh);
    }

    @Test
    public void testLoggerReferenceHandlerRuntimeException() throws Exception
    {
        stub(referenceQueue.remove()).toThrow(new RuntimeException("mock exception"));
        LoggerReferenceHandler lrh = new LoggerReferenceHandler("thread", referenceQueue, references, loggerRepository);
        assertNotNull(lrh);
    }

}


