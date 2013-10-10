/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


