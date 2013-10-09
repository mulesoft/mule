/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    @IntegrationBean
    private TestExceptionIBean test;

    @Test
    public void testExceptionIsCaughtByListener() throws Exception
    {
        final AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        test.setExceptionListener(new ExceptionListener()
        {
            @Override
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
            @Override
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

