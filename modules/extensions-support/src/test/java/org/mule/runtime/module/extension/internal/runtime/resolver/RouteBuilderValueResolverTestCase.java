/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class RouteBuilderValueResolverTestCase {

  @Mock
  private ObjectBuilder objectBuilder;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  @Mock
  private MessageProcessorChain chain;

  @Test
  public void routeInitialiseIsPropagatedToItsChain() throws MuleException {
    RouteBuilderValueResolver routeBuilder = new RouteBuilderValueResolver(objectBuilder, muleContext, chain);
    routeBuilder.initialise();
    verify(chain, times(1)).initialise();
  }

  @Test
  public void routeStartIsPropagatedToItsChain() throws MuleException {
    RouteBuilderValueResolver routeBuilder = new RouteBuilderValueResolver(objectBuilder, muleContext, chain);
    routeBuilder.start();
    verify(chain, times(1)).start();
  }

  @Test
  public void routeStopIsPropagatedToItsChain() throws MuleException {
    RouteBuilderValueResolver routeBuilder = new RouteBuilderValueResolver(objectBuilder, muleContext, chain);
    routeBuilder.stop();
    verify(chain, times(1)).stop();
  }

  @Test
  public void routeDisposeIsPropagatedToItsChain() {
    RouteBuilderValueResolver routeBuilder = new RouteBuilderValueResolver(objectBuilder, muleContext, chain);
    routeBuilder.dispose();
    verify(chain, times(1)).dispose();
  }
}
