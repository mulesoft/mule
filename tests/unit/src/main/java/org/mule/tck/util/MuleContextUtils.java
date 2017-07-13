/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR_LOCATION;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.UUID;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

/**
 * Provides helper methods to handle mock {@link MuleContext}s in unit tests.
 *
 * @since 4.0
 */
public class MuleContextUtils {

  private MuleContextUtils() {
    // No instances of this class allowed
  }

  public static MuleContext mockMuleContext() {
    final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    when(muleContext.getUniqueIdString()).thenReturn(UUID.getUUID());
    when(muleContext.getDefaultErrorHandler()).thenReturn(new OnErrorPropagateHandler());
    StreamingManager streamingManager = mock(StreamingManager.class, RETURNS_DEEP_STUBS);
    try {
      final MuleRegistry registry = muleContext.getRegistry();
      doReturn(streamingManager).when(registry).lookupObject(StreamingManager.class);
    } catch (RegistrationException e) {
      throw new RuntimeException(e);
    }

    return muleContext;
  }

  /**
   * Creates and configures a mock {@link MuleContext} to return testing services implementations.
   * 
   * @return the created {@code muleContext}.
   */
  public static MuleContext mockContextWithServices() {
    final MuleContext muleContext = mockMuleContext();
    when(muleContext.getSchedulerService()).thenReturn(spy(new SimpleUnitTestSupportSchedulerService()));
    return muleContext;
  }

  /**
   * Creates a basic event builder with its context already set.
   * 
   * @return a basic event builder with its context already set.
   */
  public static Builder eventBuilder() throws MuleException {
    FlowConstruct flowConstruct = getTestFlow(mockContextWithServices());
    return Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR_LOCATION));
  }

}
