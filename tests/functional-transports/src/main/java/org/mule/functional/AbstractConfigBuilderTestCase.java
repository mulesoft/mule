/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.runtime.core.routing.filters.MessagePropertyFilter;

import org.junit.Test;

public abstract class AbstractConfigBuilderTestCase extends AbstractScriptConfigBuilderTestCase {

  public AbstractConfigBuilderTestCase(boolean legacy) {
    super(legacy);
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }


  @Override
  public void testGlobalEndpointConfig() throws MuleException {
    super.testGlobalEndpointConfig();
    ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint("fruitBowlEndpoint");
    assertNotNull(endpoint);
    assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");

    MessagePropertyFilter filter = (MessagePropertyFilter) endpoint.getFilter();
    assertNotNull(filter);
    assertEquals("foo=bar", filter.getPattern());
  }

  @Test
  public void testThreadingConfig() throws DefaultMuleException {
    // expected default values from the configuration;
    // these should differ from the programmatic values!

    // globals
    int defaultMaxBufferSize = 42;
    int defaultMaxThreadsActive = 16;
    int defaultMaxThreadsIdle = 3;
    // WAIT is 0, RUN is 4
    int defaultThreadPoolExhaustedAction = ThreadingProfile.WHEN_EXHAUSTED_WAIT;
    int defaultThreadTTL = 60001;

    // for the connector
    int connectorMaxBufferSize = 2;

    // for the service
    int componentMaxBufferSize = 6;
    int componentMaxThreadsActive = 12;
    int componentMaxThreadsIdle = 6;
    int componentThreadPoolExhaustedAction = ThreadingProfile.WHEN_EXHAUSTED_DISCARD;

    // test default config
    ThreadingProfile tp = muleContext.getDefaultThreadingProfile();
    assertEquals(defaultMaxBufferSize, tp.getMaxBufferSize());
    assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
    assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
    assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
    assertEquals(defaultThreadTTL, tp.getThreadTTL());

    // test service threading profile defaults
    tp = muleContext.getDefaultServiceThreadingProfile();
    assertEquals(defaultMaxBufferSize, tp.getMaxBufferSize());
    assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
    assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
    assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
    assertEquals(defaultThreadTTL, tp.getThreadTTL());

    // test that unset values retain a default value
    AbstractConnector c = (AbstractConnector) muleContext.getRegistry().lookupObject("dummyConnector");
    tp = c.getDispatcherThreadingProfile();
    // this value is configured
    assertEquals(connectorMaxBufferSize, tp.getMaxBufferSize());
    // these values are inherited
    assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
    assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
    assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
    assertEquals(defaultThreadTTL, tp.getThreadTTL());

    // test per-service values
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
    AsynchronousProcessingStrategy processingStrategy = (AsynchronousProcessingStrategy) flow.getProcessingStrategy();
    // these values are configured
    assertEquals(componentMaxBufferSize, processingStrategy.getMaxBufferSize().intValue());
    assertEquals(componentMaxThreadsActive, processingStrategy.getMaxThreads().intValue());
    assertEquals(componentThreadPoolExhaustedAction, processingStrategy.getPoolExhaustedAction().intValue());
    // this value is inherited
    assertEquals(defaultThreadTTL, tp.getThreadTTL());
  }

  @Test
  public void testEndpointProperties() throws Exception {
    // test transaction config
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
    InboundEndpoint inEndpoint = (InboundEndpoint) ((CompositeMessageSource) flow.getMessageSource()).getSources().get(1);
    assertNotNull(inEndpoint);
    assertNotNull(inEndpoint.getProperties());
    assertEquals("Prop1", inEndpoint.getProperties().get("testEndpointProperty"));
  }

  @Override
  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
