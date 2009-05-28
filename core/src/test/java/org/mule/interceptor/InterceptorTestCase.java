/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptor;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.interceptor.Invocation;
import org.mule.api.service.Service;
import org.mule.component.AbstractComponent;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class InterceptorTestCase extends AbstractMuleTestCase
{

    private final String BEFORE = "Before";
    private final String AFTER = "After";
    private final String COMPONENT = "component";
    private final String INTERCEPTOR_ONE = "inteceptor1";
    private final String INTERCEPTOR_TWO = "inteceptor2";
    private final String INTERCEPTOR_THREE = "inteceptor3";

    private final String SINGLE_INTERCEPTOR_RESULT = INTERCEPTOR_ONE + BEFORE + COMPONENT + INTERCEPTOR_ONE
                                                     + AFTER;
    private final String MULTIPLE_INTERCEPTOR_RESULT = INTERCEPTOR_ONE + BEFORE + INTERCEPTOR_TWO + BEFORE
                                                       + INTERCEPTOR_THREE + BEFORE + COMPONENT
                                                       + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER
                                                       + INTERCEPTOR_ONE + AFTER;

    public void testSingleInterceptor() throws Exception
    {
        Service service = getTestService();
        TestComponent component = new TestComponent();
        service.setComponent(component);
        List interceptors = new ArrayList();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        component.setInterceptors(interceptors);
        service.start();

        MuleMessage result = service.sendEvent(getTestInboundEvent(""));

        assertEquals(SINGLE_INTERCEPTOR_RESULT, result.getPayloadAsString());
    }

    public void testMultipleInterceptor() throws Exception
    {
        Service service = getTestService();
        TestComponent component = new TestComponent();
        service.setComponent(component);
        List interceptors = new ArrayList();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        interceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        interceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        component.setInterceptors(interceptors);
        service.start();

        MuleMessage result = service.sendEvent(getTestInboundEvent(""));

        assertEquals(MULTIPLE_INTERCEPTOR_RESULT, result.getPayloadAsString());
    }

    public void testSingleInterceptorStack() throws Exception
    {
        Service service = getTestService();
        TestComponent component = new TestComponent();
        service.setComponent(component);
        List interceptors = new ArrayList();
        List stackInterceptors = new ArrayList();
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        interceptors.add(new InterceptorStack(stackInterceptors));
        component.setInterceptors(interceptors);
        service.start();

        MuleMessage result = service.sendEvent(getTestInboundEvent(""));

        assertEquals(SINGLE_INTERCEPTOR_RESULT, result.getPayloadAsString());
    }

    public void testMultipleInterceptorStack() throws Exception
    {
        Service service = getTestService();
        TestComponent component = new TestComponent();
        service.setComponent(component);
        List interceptors = new ArrayList();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        List stackInterceptors = new ArrayList();
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        interceptors.add(new InterceptorStack(stackInterceptors));
        component.setInterceptors(interceptors);
        service.start();

        MuleMessage result = service.sendEvent(getTestInboundEvent(""));

        assertEquals(MULTIPLE_INTERCEPTOR_RESULT, result.getPayloadAsString());
    }

    public void testMultipleInterceptorStack2() throws Exception
    {
        Service service = getTestService();
        TestComponent component = new TestComponent();
        service.setComponent(component);
        List interceptors = new ArrayList();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        interceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        interceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        List stackInterceptors = new ArrayList();
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        interceptors.add(new InterceptorStack(stackInterceptors));
        component.setInterceptors(interceptors);
        service.start();

        MuleMessage result = service.sendEvent(getTestInboundEvent(""));

        assertEquals(INTERCEPTOR_ONE + BEFORE + INTERCEPTOR_TWO + BEFORE + INTERCEPTOR_THREE + BEFORE
                     + INTERCEPTOR_ONE + BEFORE + INTERCEPTOR_TWO + BEFORE + INTERCEPTOR_THREE + BEFORE
                     + COMPONENT + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER + INTERCEPTOR_ONE
                     + AFTER + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER + INTERCEPTOR_ONE + AFTER,
            result.getPayloadAsString());
    }

    class TestInterceptor extends EnvelopeInterceptor
    {

        private String name;

        public TestInterceptor(String name)
        {
            this.name = name;
        }

        @Override
        public void after(Invocation invocation)
        {
            try
            {
                invocation.setMessage(new DefaultMuleMessage(invocation.getMessage().getPayloadAsString()
                                                             + name + AFTER));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }

        @Override
        public void before(Invocation invocation)
        {
            try
            {
                invocation.setMessage(new DefaultMuleMessage(invocation.getMessage().getPayloadAsString()
                                                             + name + BEFORE));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    class TestComponent extends AbstractComponent
    {
        @Override
        protected Object doInvoke(MuleEvent event) throws Exception
        {
            return event.getMessageAsString() + COMPONENT;
        }
    }

}
