/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.component.InterfaceBinding;
import org.mule.compatibility.core.api.component.JavaWithBindingsComponent;
import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.functional.extensions.UsesHttpExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.exception.AbstractExceptionListener;
import org.mule.tck.ThreadingProfileConfigurationBuilder;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;

import java.util.List;

import org.junit.Test;

public abstract class AbstractScriptWithBindingsConfigBuilderTestCase extends UsesHttpExtensionFunctionalTestCase {

  // use legacy entry point resolver?
  private boolean legacy;

  protected AbstractScriptWithBindingsConfigBuilderTestCase() {
    this(false);
  }

  protected AbstractScriptWithBindingsConfigBuilderTestCase(boolean legacy) {
    this.legacy = legacy;
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new ThreadingProfileConfigurationBuilder());
  }

  @Test
  public void testEndpointConfig() throws MuleException {
    // test that targets have been resolved on targets
    ImmutableEndpoint endpoint = getEndpointFactory(muleContext).getInboundEndpoint("waterMelonEndpoint");
    assertThat(endpoint, notNullValue());
    // aliases no longer possible
    assertThat(endpoint.getEndpointURI().getAddress(), equalTo("test.queue"));

    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
    ImmutableEndpoint ep = (ImmutableEndpoint) ((CompositeMessageSource) flow.getMessageSource()).getSources().get(0);
    assertNotNull(ep);
    final List responseTransformers = ep.getResponseMessageProcessors();
    assertThat(responseTransformers, notNullValue());
    assertThat(responseTransformers.isEmpty(), is(false));
    final Object responseTransformer = responseTransformers.get(0);
    assertThat(responseTransformer, instanceOf(MessageProcessorChain.class));
    assertThat(((MessageProcessorChain) responseTransformer).getMessageProcessors().get(0),
               instanceOf(TestCompressionTransformer.class));
  }

  @Test
  public void testBindingConfig() {
    // test outbound message router
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
    assertNotNull(flow.getMessageProcessors().get(0));
    assertTrue((flow.getMessageProcessors().get(0) instanceof JavaComponent));
    List<InterfaceBinding> bindings = ((JavaWithBindingsComponent) flow.getMessageProcessors().get(0)).getInterfaceBindings();
    assertNotNull(bindings);

    assertEquals(2, bindings.size());
    // check first Router
    InterfaceBinding route1 = bindings.get(0);
    assertEquals(FruitCleaner.class, route1.getInterface());
    assertEquals("wash", route1.getMethod());
    assertNotNull(route1.getEndpoint());
    // check second Router
    InterfaceBinding route2 = bindings.get(1);
    assertEquals(FruitCleaner.class, route2.getInterface());
    assertEquals("polish", route2.getMethod());
    assertNotNull(route1.getEndpoint());
  }

  @Test
  public void testExceptionStrategy() {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
    assertNotNull(flow.getExceptionListener());

    assertTrue(((AbstractExceptionListener) flow.getExceptionListener()).getMessageProcessors().size() > 0);
    OutboundEndpoint ep =
        (OutboundEndpoint) ((AbstractExceptionListener) flow.getExceptionListener()).getMessageProcessors().get(0);

    assertEquals("test://orange.exceptions", ep.getEndpointURI().toString());
  }


  private static EndpointFactory getEndpointFactory(MuleContext muleContext) {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
