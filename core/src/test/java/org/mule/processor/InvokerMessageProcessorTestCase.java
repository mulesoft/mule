/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

public class InvokerMessageProcessorTestCase extends AbstractMuleTestCase
{

    private InvokerMessageProcessor invoker;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        invoker = new InvokerMessageProcessor();
        invoker.setObject(new TestInvokeObject());
    }

    public void testMethodFound() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod");
        invoker.setArgumentExpressionsString("#[string:1],#[string:2],#[string:3],#[string:4],#[string:5],#[string:6],#[string:7],#[string:8],#[string:true],#[string:true],#[string:11]");
        invoker.initialise();
        invoker.process(getTestEvent(""));
    }

    public void testMethodNameNotFound() throws MuleException, Exception
    {
        invoker.setMethodName("testMethodNotHere");
        invoker.setArgumentExpressionsString("#[string:1]");
        try
        {
            invoker.initialise();
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(InitialisationException.class, e.getClass());
        }
    }

    public void testMethodWithArgsNotFound() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod");
        invoker.setArgumentExpressionsString("#[string:1]");
        try
        {
            invoker.initialise();
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(InitialisationException.class, e.getClass());
        }
    }

    public void testCantTransform() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod2");
        invoker.setArgumentExpressionsString("#[string:1]");
        invoker.initialise();
        try
        {
            invoker.process(getTestEvent(""));
            fail("Exception expected");
        }
        catch (Exception e)
        {
            assertEquals(MessagingException.class, e.getClass());
            assertEquals(TransformerException.class, e.getCause().getClass());
        }
    }

    public void testReplacePayload() throws MuleException, Exception
    {
        invoker.setMethodName("testMethod3");
        invoker.setArgumentExpressionsString("#[payload]");
        invoker.initialise();
        assertEquals("hello echo", invoker.process(getTestEvent("hello")).getMessageAsString());
    }

    private class TestInvokeObject
    {

        public void testMethod(Integer arg1,
                               int arg2,
                               Long arg3,
                               long arg4,
                               Double arg5,
                               double arg6,
                               Float arg7,
                               float arg8,
                               Boolean arg9,
                               boolean arg10,
                               String arg11)
        {
        }

        public void testMethod2(Apple apple)
        {

        }

        public String testMethod3(String text)
        {
            return text + " echo";
        }

    }

}
