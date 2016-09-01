/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.lifecycle.Initialisable;
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class ExpressionLanguageEnrichmentTestCase extends AbstractELTestCase {

  public ExpressionLanguageEnrichmentTestCase(Variant variant, String mvelOptimizer) {
    super(variant, mvelOptimizer);
  }

  protected DefaultExpressionManager expressionManager;
  protected MuleContext muleContext;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    expressionManager = new DefaultExpressionManager();
    muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    MuleRegistry muleRegistry = mock(MuleRegistry.class);
    when(muleContext.getConfiguration()).thenReturn(new DefaultMuleConfiguration());
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    ExpressionLanguage expressionLanguage = getExpressionLanguage();
    if (expressionLanguage instanceof Initialisable) {
      ((Initialisable) expressionLanguage).initialise();
    }
    when(muleContext.getExpressionLanguage()).thenReturn(expressionLanguage);
    when(muleRegistry.lookupObjectsForLifecycle(Mockito.any(Class.class))).thenReturn(Collections.<Object>emptyList());
    expressionManager.setMuleContext(muleContext);
    expressionManager.initialise();
  }

  @Test
  public void enrichReplacePayload() throws Exception {
    MuleEvent event = getTestEvent("foo");
    expressionManager.enrich("message.payload", event, flowConstruct, "bar");
    assertThat(event.getMessage().getPayload(), is("bar"));
  }

  @Test
  public void enrichObjectPayload() throws Exception {
    Apple apple = new Apple();
    FruitCleaner fruitCleaner = new FruitCleaner() {

      @Override
      public void wash(Fruit fruit) {}

      @Override
      public void polish(Fruit fruit) {

      }
    };
    expressionManager.enrich("message.payload.appleCleaner", getTestEvent(apple), flowConstruct, fruitCleaner);
    assertThat(fruitCleaner, is(apple.getAppleCleaner()));
  }

  @Test
  public void enrichMessageProperty() throws Exception {
    MuleEvent event = getTestEvent("foo");
    expressionManager.enrich("message.outboundProperties.foo", event, flowConstruct, "bar");
    assertThat(event.getMessage().getOutboundProperty("foo"), is("bar"));
  }

  @Test
  public void enrichMessageAttachment() throws Exception {
    DataHandler dataHandler = new DataHandler(new Object(), "test/xml");
    MuleEvent event = getTestEvent("foo");
    expressionManager.enrich("message.outboundAttachments.foo", event, flowConstruct, dataHandler);
    assertThat(event.getMessage().getOutboundAttachment("foo"), is(dataHandler));
  }

  @Test
  public void enrichFlowVariable() throws Exception {
    Flow flow = new Flow("flow", muleContext);
    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR))
        .message(MuleMessage.builder().payload("").build()).exchangePattern(ONE_WAY).flow(flow).build();
    expressionManager.enrich("flowVars['foo']", event, flowConstruct, "bar");
    assertThat(event.getFlowVariable("foo"), is("bar"));
    assertThat(event.getSession().getProperty("foo"), nullValue());
  }

  @Test
  public void enrichSessionVariable() throws Exception {
    Flow flow = new Flow("flow", muleContext);
    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR))
        .message(MuleMessage.builder().payload("").build()).exchangePattern(ONE_WAY).flow(flow).build();
    expressionManager.enrich("sessionVars['foo']", event, flowConstruct, "bar");
    assertThat(event.getSession().getProperty("foo"), equalTo("bar"));
    assertThat(event.getFlowVariableNames(), not(contains("foo")));
  }

  @Test
  public void enrichWithDolarPlaceholder() throws Exception {
    MuleEvent event = getTestEvent("");
    expressionManager.enrich("message.outboundProperties.put('foo', $)", event, flowConstruct, "bar");
    assertThat(event.getMessage().getOutboundProperty("foo"), is("bar"));
  }

}
