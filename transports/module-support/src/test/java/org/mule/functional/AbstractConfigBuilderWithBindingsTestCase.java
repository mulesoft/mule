/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.component.AbstractComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.interceptor.InterceptorStack;
import org.mule.runtime.core.interceptor.LoggingInterceptor;
import org.mule.runtime.core.interceptor.TimerInterceptor;

import org.junit.Test;

public abstract class AbstractConfigBuilderWithBindingsTestCase extends AbstractScriptWithBindingsConfigBuilderTestCase {

  public AbstractConfigBuilderWithBindingsTestCase(boolean legacy) {
    super(legacy);
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Test
  public void testInterceptors() {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
    AbstractComponent component = (AbstractComponent) flow.getMessageProcessors().get(0);
    assertEquals(3, component.getInterceptors().size());
    assertEquals(LoggingInterceptor.class, component.getInterceptors().get(0).getClass());
    assertEquals(InterceptorStack.class, component.getInterceptors().get(1).getClass());
    assertEquals(TimerInterceptor.class, component.getInterceptors().get(2).getClass());
  }

  @Override
  public void testEndpointConfig() throws MuleException {
    super.testEndpointConfig();

    // test that targets have been resolved on targets
    ImmutableEndpoint endpoint = getEndpointFactory(muleContext).getInboundEndpoint("waterMelonEndpoint");
    assertNotNull(endpoint);
    assertEquals(UTF_8, endpoint.getEncoding());
    assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

    FlowConstruct service = muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
    assertNotNull(service);
  }

  private static EndpointFactory getEndpointFactory(MuleContext muleContext) {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
