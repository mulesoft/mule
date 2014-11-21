/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

@SmallTest
public class FlowRefFactoryBeanTestCase extends AbstractMuleTestCase
{

    private static final String STATIC_REFERENCED_FLOW = "staticReferencedFlow";
    private static final String DYNAMIC_REFERENCED_FLOW = "dynamicReferencedFlow";
    private static final String DYNAMIC_NON_EXISTANT = "#['nonExistant']";
    private static final String NON_EXISTANT = "nonExistant";

    private MuleEvent result = mock(MuleEvent.class);
    private ProcessableFlowConstruct targetFlow = mock(ProcessableFlowConstruct.class);
    private InitializableMessageProcessor targetSubFlow = mock(InitializableMessageProcessor.class);
    private ApplicationContext applicationContext = mock(ApplicationContext.class);
    private MuleContext muleContext = mock(MuleContext.class);
    private ExpressionManager expressionManager = mock(ExpressionManager.class);

    @Before
    public void setup() throws MuleException
    {
        when(muleContext.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.isExpression(anyString())).thenReturn(true);
        when(targetFlow.process(any(MuleEvent.class))).thenReturn(result);
        when(targetSubFlow.process(any(MuleEvent.class))).thenReturn(result);
    }

    @Test
    public void testStaticFlowRefFlow() throws Exception
    {
        // Flow is wrapped to prevent lifecycle propagation
        FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetFlow);

        assertNotSame(targetFlow, flowRefFactoryBean.getObject());
        assertNotSame(targetFlow, flowRefFactoryBean.getObject());

        verifyProcess(flowRefFactoryBean, targetFlow, 0);
    }

    @Test
    public void testDynamicFlowRefFlow() throws Exception
    {
        // Inner MessageProcessor is used to resolve MP in runtime
        FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetFlow);

        assertNotSame(targetFlow, flowRefFactoryBean.getObject());
        assertNotSame(targetFlow, flowRefFactoryBean.getObject());

        verifyProcess(flowRefFactoryBean, targetFlow, 0);
    }

    @Test
    public void testStaticFlowRefSubFlow() throws Exception
    {
        FlowRefFactoryBean flowRefFactoryBean = createStaticFlowRefFactoryBean(targetSubFlow);

        assertEquals(targetSubFlow, flowRefFactoryBean.getObject());
        assertEquals(targetSubFlow, flowRefFactoryBean.getObject());

        verifyProcess(flowRefFactoryBean, targetSubFlow, 0);
    }

    @Test
    public void testDynamicFlowRefSubFlow() throws Exception
    {
        FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlow);

        // Inner MessageProcessor is used to resolve MP in runtime
        assertNotSame(targetSubFlow, flowRefFactoryBean.getObject());
        assertNotSame(targetSubFlow, flowRefFactoryBean.getObject());

        verifyProcess(flowRefFactoryBean, targetSubFlow, 1);
    }

    @Test
    public void testDynamicFlowRefSubFlowConstructAware() throws Exception
    {
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        MuleEvent event = mock(MuleEvent.class);
        when(event.getFlowConstruct()).thenReturn(flowConstruct);
        FlowConstructAwareProcessor targetSubFlowConstructAware = mock(FlowConstructAwareProcessor.class);
        when(targetSubFlowConstructAware.process(event)).thenReturn(result);

        FlowRefFactoryBean flowRefFactoryBean = createDynamicFlowRefFactoryBean(targetSubFlowConstructAware);
        assertSame(result, flowRefFactoryBean.getObject().process(event));

        verify(targetSubFlowConstructAware).setFlowConstruct(flowConstruct);
    }

    @Test(expected = MuleRuntimeException.class)
    public void testStaticFlowRefDoesNotExist() throws Exception
    {
        when(expressionManager.isExpression(anyString())).thenReturn(false);

        createFlowRefFactoryBean(NON_EXISTANT).getObject();
    }

    @Test(expected = MuleRuntimeException.class)
    public void testDynamicFlowRefDoesNotExist() throws Exception
    {
        when(expressionManager.isExpression(anyString())).thenReturn(true);
        when(expressionManager.parse(eq(DYNAMIC_NON_EXISTANT), any(MuleEvent.class))).thenReturn("other");

        createFlowRefFactoryBean(DYNAMIC_NON_EXISTANT).getObject().process(mock(MuleEvent.class));
    }

    private FlowRefFactoryBean createFlowRefFactoryBean(String name) throws InitialisationException
    {
        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName(name);
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();
        return flowRefFactoryBean;
    }

    private FlowRefFactoryBean createStaticFlowRefFactoryBean(InitializableMessageProcessor target) throws InitialisationException
    {
        when(expressionManager.isExpression(anyString())).thenReturn(false);
        when(applicationContext.getBean(eq(STATIC_REFERENCED_FLOW))).thenReturn(target);

        return createFlowRefFactoryBean(STATIC_REFERENCED_FLOW);
    }

    private FlowRefFactoryBean createDynamicFlowRefFactoryBean(InitializableMessageProcessor target) throws InitialisationException
    {
        when(expressionManager.isExpression(anyString())).thenReturn(true);
        when(expressionManager.parse(eq(DYNAMIC_REFERENCED_FLOW), any(MuleEvent.class))).thenReturn(
                "parsedDynamicReferencedFlow");
        when(applicationContext.getBean(eq("parsedDynamicReferencedFlow"))).thenReturn(target);

        return createFlowRefFactoryBean(DYNAMIC_REFERENCED_FLOW);
    }

    private void verifyProcess(FlowRefFactoryBean flowRefFactoryBean, InitializableMessageProcessor target, int lifecycleRounds) throws Exception
    {
        assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));
        assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));

        verify(applicationContext).getBean(anyString());

        verify(target, times(2)).process(any(MuleEvent.class));
        verify(target, times(lifecycleRounds)).initialise();

        flowRefFactoryBean.dispose();
        verify(target, times(lifecycleRounds)).dispose();
    }

    interface InitializableMessageProcessor extends MessageProcessor, Initialisable, Disposable
    {
    }

    interface ProcessableFlowConstruct extends InitializableMessageProcessor, FlowConstruct
    {
    }

    interface FlowConstructAwareProcessor extends InitializableMessageProcessor, FlowConstructAware
    {
    }

}
