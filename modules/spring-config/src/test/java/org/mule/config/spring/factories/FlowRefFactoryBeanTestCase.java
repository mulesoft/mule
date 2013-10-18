/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

@SmallTest
public class FlowRefFactoryBeanTestCase extends AbstractMuleTestCase
{

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
        when(expressionManager.isExpression(Mockito.anyString())).thenReturn(true);
        when(targetFlow.process(Mockito.any(MuleEvent.class))).thenReturn(result);
        when(targetSubFlow.process(Mockito.any(MuleEvent.class))).thenReturn(result);
    }

    @Test
    public void testStaticFlowRefFlow() throws Exception
    {
        when(expressionManager.isExpression(Mockito.anyString())).thenReturn(false);
        when(applicationContext.getBean(Mockito.eq("staticReferencedFlow"))).thenReturn(targetFlow);

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("staticReferencedFlow");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();

        // Flow is wrapped to prevent lifecycle propagagtion
        assertNotSame(targetFlow, flowRefFactoryBean.getObject());
        assertNotSame(targetFlow, flowRefFactoryBean.getObject());

        Assert.assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));

        Mockito.verify(applicationContext, Mockito.times(1)).getBean(Mockito.anyString());

        Mockito.verify(targetFlow, Mockito.times(1)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(targetFlow, Mockito.never()).initialise();

        flowRefFactoryBean.dispose();
        Mockito.verify(targetSubFlow, Mockito.never()).dispose();
    }

    @Test
    public void testDynamicFlowRefFlow() throws Exception
    {
        when(expressionManager.isExpression(Mockito.anyString())).thenReturn(true);

        when(expressionManager.parse(Mockito.eq("dynamicReferencedFlow"), Mockito.any(MuleEvent.class))).thenReturn(
            "parsedDynamicReferencedFlow");

        when(applicationContext.getBean(Mockito.eq("parsedDynamicReferencedFlow"))).thenReturn(targetFlow);

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("dynamicReferencedFlow");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();

        // Inner MessageProcessor is used to resolve MP in runtime
        Assert.assertNotSame(targetFlow, flowRefFactoryBean.getObject());
        Assert.assertNotSame(targetFlow, flowRefFactoryBean.getObject());

        Assert.assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));
        Assert.assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));

        Mockito.verify(applicationContext, Mockito.times(1)).getBean(Mockito.anyString());

        Mockito.verify(targetFlow, Mockito.times(2)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(targetFlow, Mockito.never()).initialise();

        flowRefFactoryBean.dispose();
        Mockito.verify(targetSubFlow, Mockito.never()).dispose();
    }

    @Test
    public void testStaticFlowRefSubFlow() throws Exception
    {
        when(expressionManager.isExpression(Mockito.anyString())).thenReturn(false);
        when(applicationContext.getBean(Mockito.eq("staticReferencedFlow"))).thenReturn(targetSubFlow);

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("staticReferencedFlow");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();

        assertEquals(targetSubFlow, flowRefFactoryBean.getObject());
        assertEquals(targetSubFlow, flowRefFactoryBean.getObject());

        Assert.assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));
        Assert.assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));

        Mockito.verify(applicationContext, Mockito.times(1)).getBean(Mockito.anyString());

        Mockito.verify(targetSubFlow, Mockito.times(2)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(targetSubFlow, Mockito.never()).initialise();

        flowRefFactoryBean.dispose();
        Mockito.verify(targetSubFlow, Mockito.never()).dispose();
    }

    @Test
    public void testDynamicFlowRefSubFlow() throws Exception
    {
        when(expressionManager.isExpression(Mockito.anyString())).thenReturn(true);

        when(expressionManager.parse(Mockito.eq("dynamicReferencedFlow"), Mockito.any(MuleEvent.class))).thenReturn(
            "parsedDynamicReferencedFlow");

        when(applicationContext.getBean(Mockito.eq("parsedDynamicReferencedFlow"))).thenReturn(targetSubFlow);

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("dynamicReferencedFlow");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();

        // Inner MessageProcessor is used to resolve MP in runtime
        Assert.assertNotSame(targetSubFlow, flowRefFactoryBean.getObject());
        Assert.assertNotSame(targetSubFlow, flowRefFactoryBean.getObject());

        Assert.assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));
        Assert.assertSame(result, flowRefFactoryBean.getObject().process(mock(MuleEvent.class)));

        Mockito.verify(applicationContext, Mockito.times(1)).getBean(Mockito.anyString());

        Mockito.verify(targetSubFlow, Mockito.times(2)).process(Mockito.any(MuleEvent.class));
        Mockito.verify(targetSubFlow, Mockito.times(1)).initialise();

        flowRefFactoryBean.dispose();
        Mockito.verify(targetSubFlow, Mockito.times(1)).dispose();
    }

    @Test(expected = MuleRuntimeException.class)
    public void testStaticFlowRefDoesNotExist() throws Exception
    {
        when(expressionManager.isExpression(Mockito.anyString())).thenReturn(false);

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("nonExistant");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();
        flowRefFactoryBean.getObject();
    }

    @Test(expected = MuleRuntimeException.class)
    public void testDynamicFlowRefDoesNotExist() throws Exception
    {
        when(expressionManager.isExpression(Mockito.anyString())).thenReturn(true);
        when(expressionManager.parse(Mockito.eq("#['nonExistant']"), Mockito.any(MuleEvent.class))).thenReturn(
            "other");

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("#['nonExistant']");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();
        flowRefFactoryBean.getObject().process(mock(MuleEvent.class));
    }

    interface InitializableMessageProcessor extends MessageProcessor, Initialisable, Disposable
    {
    }

    interface ProcessableFlowConstruct extends MessageProcessor, FlowConstruct, Initialisable, Disposable
    {
    }

}
