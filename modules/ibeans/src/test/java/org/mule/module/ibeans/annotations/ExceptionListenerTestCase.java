/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import java.beans.ExceptionListener;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests using an exception listener to intercept all exceptions on the ibean.  Also test that parsing the ibean will not barf if
 * the ibean extends {@link org.ibeans.api.ExceptionListenerAware}
 */
public class ExceptionListenerTestCase extends AbstractIBeansTestCase
{
    @SuppressWarnings("unused")    
    @IntegrationBean
    private TestExceptionIBean test;


    @Test
    public void testExceptionIsCaughtByListener() throws Exception
    {
        final AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        test.setExceptionListener(new ExceptionListener()
        {
            public void exceptionThrown(Exception e)
            {
                exceptionThrown.set(true);
            }
        });
        String data = test.doSomething("blah");
        //Exception should not be thrown, instead the listener intercepts it
        assertTrue(exceptionThrown.get());
        assertNull(data);
    }

    @Test
    public void testExceptionOfDifferentTypeIsCaughtByListener() throws Exception
    {
        final AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        test.setExceptionListener(new ExceptionListener()
        {
            public void exceptionThrown(Exception e)
            {
                exceptionThrown.set(true);
                assertTrue(e instanceof UnknownHostException);
            }
        });
        String data = test.doSomethingElse();
        //Exception should not be thrown, instead the listener intercepts it
        assertTrue(exceptionThrown.get());
        assertNull(data);
    }
}

