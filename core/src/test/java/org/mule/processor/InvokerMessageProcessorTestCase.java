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

import org.mule.api.MuleException;
import org.mule.tck.AbstractMuleTestCase;

public class InvokerMessageProcessorTestCase extends AbstractMuleTestCase
{

    private InvokerMessageProcessor invoker;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        invoker = new InvokerMessageProcessor();
        invoker.setObject(new TestInvokeObject());
        invoker.setMethodName("testMethod");
    }

    public void testProcess() throws MuleException, Exception
    {
        invoker.setArgumentExpressionsString("#[string:1],#[string:2],#[string:3],#[string:4],#[string:5],#[string:6],#[string:7],#[string:8],#[string:true],#[string:true],#[string:11]");
        invoker.initialise();
        invoker.process(getTestEvent(""));
    }

    public void testMethod(String arg1, int arg2, int arg3)
    {
        System.out.println(arg1 + arg2 + arg3);
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
            System.out.println(arg1 + arg2 + arg3);
        }

    }

}
