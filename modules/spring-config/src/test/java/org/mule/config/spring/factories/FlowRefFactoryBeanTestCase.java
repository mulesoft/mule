/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.expression.ExpressionManager;
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

    private MuleEvent result;
    private MessageProcessor target;
    private ApplicationContext applicationContext;
    private MuleContext muleContext;
    private ExpressionManager expressionManager;

    @Before
    public void setup() throws MuleException
    {
        muleContext = Mockito.mock(MuleContext.class);
        expressionManager = Mockito.mock(ExpressionManager.class);
        Mockito.when(muleContext.getExpressionManager()).thenReturn(expressionManager);
        Mockito.when(expressionManager.isExpression(Mockito.anyString())).thenReturn(true);
        result = Mockito.mock(MuleEvent.class);
        target = Mockito.mock(MessageProcessor.class);
        Mockito.when(target.process(Mockito.any(MuleEvent.class))).thenReturn(result);
        applicationContext = Mockito.mock(ApplicationContext.class);
    }

    @Test
    public void testStaticFlowRef() throws Exception
    {
        Mockito.when(expressionManager.isExpression(Mockito.anyString())).thenReturn(false);
        Mockito.when(applicationContext.getBean(Mockito.eq("staticReferencedFlow"))).thenReturn(target);

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("staticReferencedFlow");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();
        Assert.assertEquals(target, flowRefFactoryBean.getObject());
    }

    @Test
    public void testDynamicFlowRef() throws Exception
    {
        Mockito.when(expressionManager.isExpression(Mockito.anyString())).thenReturn(true);

        Mockito.when(
            expressionManager.parse(Mockito.eq("dynamicReferencedFlow"), Mockito.any(MuleEvent.class)))
            .thenReturn("parsedDynamicReferencedFlow");

        Mockito.when(applicationContext.getBean(Mockito.eq("parsedDynamicReferencedFlow")))
            .thenReturn(target);
        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("dynamicReferencedFlow");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();
        Assert.assertNotSame(target, flowRefFactoryBean.getObject());
        Assert.assertSame(result,
            ((MessageProcessor) flowRefFactoryBean.getObject()).process(Mockito.mock(MuleEvent.class)));
    }

    @Test(expected = MuleRuntimeException.class)
    public void testStaticFlowRefDoesntExist() throws Exception
    {
        Mockito.when(expressionManager.isExpression(Mockito.anyString())).thenReturn(false);

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("nonExistant");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();
        flowRefFactoryBean.getObject();
    }

    @Test(expected = MuleRuntimeException.class)
    public void testDynamiccFlowRefDoesntExist() throws Exception
    {
        Mockito.when(expressionManager.isExpression(Mockito.anyString())).thenReturn(true);
        Mockito.when(expressionManager.parse(Mockito.eq("nonExistant"), Mockito.any(MuleEvent.class)))
            .thenReturn("other");

        FlowRefFactoryBean flowRefFactoryBean = new FlowRefFactoryBean();
        flowRefFactoryBean.setName("#[nonExistant]");
        flowRefFactoryBean.setApplicationContext(applicationContext);
        flowRefFactoryBean.setMuleContext(muleContext);
        flowRefFactoryBean.initialise();
        ((MessageProcessor) flowRefFactoryBean.getObject()).process(Mockito.mock(MuleEvent.class));
    }

}
