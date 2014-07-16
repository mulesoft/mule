/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.service.Service;
import org.mule.component.AbstractComponent;
import org.mule.management.stats.ProcessingTime;
import org.mule.model.seda.SedaService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class InterceptorTestCase extends AbstractMuleContextTestCase
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

    @Test
    public void testSingleInterceptor() throws Exception
    {
        Service service = createUninitializedService();
        TestComponent component = (TestComponent) service.getComponent();

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        component.setInterceptors(interceptors);
        service.initialise();
        service.start();

        MuleEvent result = component.process(getTestEvent(""));

        assertEquals(SINGLE_INTERCEPTOR_RESULT, result.getMessageAsString());
    }

    @Test
    public void testMultipleInterceptor() throws Exception
    {
        Service service = createUninitializedService();
        TestComponent component = (TestComponent) service.getComponent();

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        interceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        interceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        component.setInterceptors(interceptors);
        service.initialise();
        service.start();

        MuleEvent result = component.process(getTestEvent(""));

        assertEquals(MULTIPLE_INTERCEPTOR_RESULT, result.getMessageAsString());
    }

    @Test
    public void testSingleInterceptorStack() throws Exception
    {
        Service service = createUninitializedService();
        TestComponent component = (TestComponent) service.getComponent();

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        List<Interceptor> stackInterceptors = new ArrayList<Interceptor>();
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        interceptors.add(new InterceptorStack(stackInterceptors));
        component.setInterceptors(interceptors);
        service.initialise();
        service.start();

        MuleEvent result = component.process(getTestEvent(""));

        assertEquals(SINGLE_INTERCEPTOR_RESULT, result.getMessageAsString());
    }

    @Test
    public void testMultipleInterceptorStack() throws Exception
    {
        Service service = createUninitializedService();
        TestComponent component = (TestComponent) service.getComponent();

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        List<Interceptor> stackInterceptors = new ArrayList<Interceptor>();
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        interceptors.add(new InterceptorStack(stackInterceptors));
        component.setInterceptors(interceptors);
        service.initialise();
        service.start();

        MuleEvent result = component.process(getTestEvent(""));

        assertEquals(MULTIPLE_INTERCEPTOR_RESULT, result.getMessageAsString());
    }

    @Test
    public void testMultipleInterceptorStack2() throws Exception
    {
        Service service = createUninitializedService();
        TestComponent component = (TestComponent) service.getComponent();

        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        interceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        interceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        List<Interceptor> stackInterceptors = new ArrayList<Interceptor>();
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_ONE));
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_TWO));
        stackInterceptors.add(new TestInterceptor(INTERCEPTOR_THREE));
        interceptors.add(new InterceptorStack(stackInterceptors));
        component.setInterceptors(interceptors);
        service.initialise();
        service.start();

        MuleEvent result = component.process(getTestEvent(""));

        assertEquals(INTERCEPTOR_ONE + BEFORE + INTERCEPTOR_TWO + BEFORE + INTERCEPTOR_THREE + BEFORE
                     + INTERCEPTOR_ONE + BEFORE + INTERCEPTOR_TWO + BEFORE + INTERCEPTOR_THREE + BEFORE
                     + COMPONENT + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER + INTERCEPTOR_ONE
                     + AFTER + INTERCEPTOR_THREE + AFTER + INTERCEPTOR_TWO + AFTER + INTERCEPTOR_ONE + AFTER,
            result.getMessageAsString());
    }

    class TestInterceptor extends AbstractEnvelopeInterceptor
    {
        private String name;

        public TestInterceptor(String name)
        {
            this.name = name;
        }

        @Override
        public MuleEvent after(MuleEvent event)
        {
            try
            {
                event.getMessage().setPayload(event.getMessage().getPayloadAsString() + name + AFTER);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
            return event;
        }

        @Override
        public MuleEvent before(MuleEvent event)
        {
            try
            {
                event.getMessage().setPayload(event.getMessage().getPayloadAsString() + name + BEFORE);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
            return event;
        }

        @Override
        public MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException
        {
            return event;
        }
    }

    protected Service createUninitializedService() throws Exception
    {
        TestComponent component = new TestComponent();
        Service service = new SedaService(muleContext);
        service.setName("name");
        service.setComponent(component);
        service.setModel(muleContext.getRegistry().lookupSystemModel());
        return service;
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
