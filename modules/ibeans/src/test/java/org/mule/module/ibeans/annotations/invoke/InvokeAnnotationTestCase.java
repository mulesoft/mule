/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations.invoke;

import org.mule.tck.AbstractMuleTestCase;

import org.ibeans.annotation.IntegrationBean;
import org.ibeans.api.CallException;

public class InvokeAnnotationTestCase extends AbstractMuleTestCase
{
    public InvokeAnnotationTestCase()
    {
        setStartContext(true);
    }

    @SuppressWarnings("unused")    
    @IntegrationBean
    private InvokeTestIBean test;

    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("testCase", this);
    }

    public void testIBeanInvoke() throws Exception {
        assertNotNull(test);
        String result = test.greet("Ross");
        assertEquals("Hello Ross", result);
    }

    public void testIBeanInvokeBadObject() throws Exception {
        try
        {
            test.greetFail1("Ross");
            fail("dummy2 not a valid object");
        }
        catch (CallException e)
        {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            //expected
        }
    }

    public void testIBeanInvokeBadMethod() throws Exception {
        try
        {
            test.greetFail2("Ross");
            fail("sayHellox not a valid method");
        }
        catch (CallException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
            //expected
        }
    }

    public void testIBeanInvokeWrongArguments() throws Exception {
        try
        {
            test.greetFail3("Ross", "UK");
            fail("Wrong number of arguments");
        }
        catch (CallException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
            //expected
        }
    }
}
