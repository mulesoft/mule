/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.el.context.AbstractELTestCase;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitCleaner;

import org.junit.Before;
import org.junit.Test;

import javax.activation.DataHandler;

@SmallTest
public class ExpressionLanguageEnrichmentTestCase extends AbstractELTestCase {

  public ExpressionLanguageEnrichmentTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  protected MVELExpressionLanguage expressionLanguage;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    expressionLanguage = new MVELExpressionLanguage(muleContext);
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(OBJECT_EXPRESSION_LANGUAGE, expressionLanguage);
  }

  @Test
  public void enrichReplacePayload() throws Exception {
    CoreEvent event = CoreEvent.builder(context).message(of("foo")).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    expressionLanguage.enrich("message.payload", event, eventBuilder, ((Component) flowConstruct).getLocation(), "bar");
    assertThat(eventBuilder.build().getMessage().getPayload().getValue(), is("bar"));
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
    CoreEvent event = CoreEvent.builder(context).message(of(apple)).build();
    expressionLanguage.enrich("message.payload.appleCleaner", event, CoreEvent.builder(event),
                              ((Component) flowConstruct).getLocation(), fruitCleaner);
    assertThat(apple.getAppleCleaner(), is(fruitCleaner));
  }

  @Test
  public void enrichMessageProperty() throws Exception {
    CoreEvent event = CoreEvent.builder(context).message(of("foo")).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    expressionLanguage.enrich("message.outboundProperties.foo", event, eventBuilder,
                              ((Component) flowConstruct).getLocation(), "bar");
    assertThat(((InternalMessage) eventBuilder.build().getMessage()).getOutboundProperty("foo"), is("bar"));
  }

  @Test
  public void enrichMessageAttachment() throws Exception {
    DataHandler dataHandler = new DataHandler(new Object(), "test/xml");
    CoreEvent event = CoreEvent.builder(context).message(of("foo")).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    expressionLanguage.enrich("message.outboundAttachments.foo", event, eventBuilder, flowConstruct.getLocation(), dataHandler);
    assertThat(((InternalMessage) eventBuilder.build().getMessage()).getOutboundAttachment("foo"), is(dataHandler));
  }

  @Test
  public void enrichFlowVariable() throws Exception {
    CoreEvent event = eventBuilder(muleContext).message(of("")).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    expressionLanguage.enrich("flowVars['foo']", event, eventBuilder, ((Component) flowConstruct).getLocation(), "bar");
    assertThat(eventBuilder.build().getVariables().get("foo").getValue(), is("bar"));
    assertThat(((PrivilegedEvent) eventBuilder.build()).getSession().getProperty("foo"), nullValue());
  }

  @Test
  public void enrichSessionVariable() throws Exception {
    CoreEvent event = eventBuilder(muleContext).message(Message.of("")).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    expressionLanguage.enrich("sessionVars['foo']", event, eventBuilder, ((Component) flowConstruct).getLocation(), "bar");
    assertThat(((PrivilegedEvent) eventBuilder.build()).getSession().getProperty("foo"), equalTo("bar"));
    assertThat(eventBuilder.build().getVariables().keySet(), not(hasItem("foo")));
  }

  @Test
  public void enrichWithDolarPlaceholder() throws Exception {
    CoreEvent event = CoreEvent.builder(context).message(of("")).build();
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    expressionLanguage.enrich("message.outboundProperties.put('foo', $)", event, eventBuilder,
                              ((Component) flowConstruct).getLocation(), "bar");

    assertThat(((InternalMessage) eventBuilder.build().getMessage()).getOutboundProperty("foo"), is("bar"));
  }

}
