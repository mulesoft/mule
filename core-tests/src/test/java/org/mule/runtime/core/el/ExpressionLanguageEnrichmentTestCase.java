/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.el.context.AbstractELTestCase;
import org.mule.runtime.core.expression.DefaultExpressionManager;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitCleaner;

import java.util.Collections;

import javax.activation.DataHandler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class ExpressionLanguageEnrichmentTestCase extends AbstractELTestCase
{

    public ExpressionLanguageEnrichmentTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    protected DefaultExpressionManager expressionManager;
    protected MuleContext muleContext;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception
    {
        expressionManager = new DefaultExpressionManager();
        muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        MuleRegistry muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        ExpressionLanguage expressionLanguage = getExpressionLanguage();
        if (expressionLanguage instanceof Initialisable)
        {
            ((Initialisable) expressionLanguage).initialise();
        }
        when(muleContext.getExpressionLanguage()).thenReturn(expressionLanguage);
        when(muleRegistry.lookupObjectsForLifecycle(Mockito.any(Class.class))).thenReturn(
            Collections.<Object> emptyList());
        expressionManager.setMuleContext(muleContext);
        expressionManager.initialise();
    }

    @Test
    public void enrichReplacePayload() throws Exception
    {
        MuleEvent event = getTestEvent("foo");
        expressionManager.enrich("message.payload", event, "bar");
        Assert.assertEquals("bar", event.getMessage().getPayload());
    }

    @Test
    public void enrichObjectPayload() throws Exception
    {
        Apple apple = new Apple();
        FruitCleaner fruitCleaner = new FruitCleaner()
        {
            @Override
            public void wash(Fruit fruit)
            {
            }

            @Override
            public void polish(Fruit fruit)
            {

            }
        };
        expressionManager.enrich("message.payload.appleCleaner", getTestEvent(apple), fruitCleaner);
        Assert.assertEquals(apple.getAppleCleaner(), fruitCleaner);
    }

    @Test
    public void enrichMessageProperty() throws Exception
    {
        MuleEvent event = getTestEvent("foo");
        expressionManager.enrich("message.outboundProperties.foo", event, "bar");
        Assert.assertEquals("bar", event.getMessage().getOutboundProperty("foo"));
    }

    @Test
    public void enrichMessageAttachment() throws Exception
    {
        DataHandler dataHandler = new DataHandler(new Object(), "test/xml");
        MuleEvent event = getTestEvent("foo");
        expressionManager.enrich("message.outboundAttachments.foo", event, dataHandler);
        Assert.assertEquals(dataHandler, event.getMessage().getOutboundAttachment("foo"));
    }

    @Test
    public void enrichFlowVariable() throws Exception
    {
        MuleEvent event = new DefaultMuleEvent(MuleMessage.builder().payload("").build(),
                MessageExchangePattern.ONE_WAY, new Flow("flow", muleContext));
        expressionManager.enrich("flowVars['foo']", event, "bar");
        Assert.assertEquals("bar", event.getFlowVariable("foo"));
        Assert.assertNull(event.getSession().getProperty("foo"));
    }

    @Test
    public void enrichSessionVariable() throws Exception
    {
        MuleEvent event = new DefaultMuleEvent(MuleMessage.builder().payload("").build(),
                MessageExchangePattern.ONE_WAY, new Flow("flow", muleContext));
        expressionManager.enrich("sessionVars['foo']", event, "bar");
        Assert.assertEquals("bar", event.getSession().getProperty("foo"));
        Assert.assertNull(event.getFlowVariable("foo"));
    }

    @Test
    public void enrichWithDolarPlaceholder() throws Exception
    {
        MuleEvent event = getTestEvent("");
        expressionManager.enrich("message.outboundProperties.put('foo', $)", event, "bar");
        Assert.assertEquals("bar", event.getMessage().getOutboundProperty("foo"));
    }

}
