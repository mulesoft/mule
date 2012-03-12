/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.expression.DefaultExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
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
public class ExpressionLanguageEnrichmentTestCase extends AbstractMuleTestCase
{

    protected DefaultExpressionManager expressionManager;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws InitialisationException
    {
        expressionManager = new DefaultExpressionManager();
        MuleContext muleContext = Mockito.mock(MuleContext.class);
        MuleRegistry muleRegistry = Mockito.mock(MuleRegistry.class);
        Mockito.when(muleContext.getRegistry()).thenReturn(muleRegistry);
        Mockito.when(muleRegistry.lookupObjectsForLifecycle(Mockito.any(Class.class))).thenReturn(
            Collections.<Object> emptyList());
        expressionManager.setMuleContext(muleContext);
        expressionManager.initialise();
    }

    @Test
    public void enrichReplacePayload()
    {
        MuleMessage message = new DefaultMuleMessage("foo", Mockito.mock(MuleContext.class));
        expressionManager.enrich("message.payload", message, "bar");
        Assert.assertEquals("bar", message.getPayload());
    }

    @Test
    public void enrichObjectPayload()
    {
        Apple apple = new Apple();
        FruitCleaner fruitCleaner = new FruitCleaner()
        {
            public void wash(Fruit fruit)
            {
            }

            @Override
            public void polish(Fruit fruit)
            {

            }
        };
        expressionManager.enrich("message.payload.appleCleaner",
            new DefaultMuleMessage(apple, Mockito.mock(MuleContext.class)), fruitCleaner);
        Assert.assertEquals(apple.getAppleCleaner(), fruitCleaner);
    }

    @Test
    public void enrichMessageProperty()
    {
        MuleMessage message = new DefaultMuleMessage("foo", Mockito.mock(MuleContext.class));
        expressionManager.enrich("message.outboundProperties.foo", message, "bar");
        Assert.assertEquals("bar", message.getOutboundProperty("foo"));
    }

    @Test
    public void enrichMessageAttachment()
    {
        DataHandler dataHandler = new DataHandler(new Object(), "test/xml");
        MuleMessage message = new DefaultMuleMessage("foo", Mockito.mock(MuleContext.class));
        expressionManager.enrich("message.outboundAttachments.foo", message, dataHandler);
        Assert.assertEquals(dataHandler, message.getOutboundAttachment("foo"));
    }

    @Test
    public void enrichWithDolarPlaceholder()
    {
        MuleMessage message = new DefaultMuleMessage("", Mockito.mock(MuleContext.class));
        expressionManager.enrich("message.outboundProperties.put('foo', $)", message, "bar");
        Assert.assertEquals("bar", message.getOutboundProperty("foo"));
    }

}
