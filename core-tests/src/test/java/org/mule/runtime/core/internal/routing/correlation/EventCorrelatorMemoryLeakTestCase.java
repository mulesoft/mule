/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing.correlation;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.runtime.core.internal.routing.EventGroup;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;


public class EventCorrelatorMemoryLeakTestCase extends AbstractMuleTestCase {

  private EventCorrelator eventCorrelator;
  private final EventCorrelatorCallback eventCorrelatorCallback = mock(EventCorrelatorCallback.class);
  private final Processor messageProcessor = mock(Processor.class);
  private PartitionableObjectStore<EventGroup> partitionableObjectStore = mock(PartitionableObjectStore.class);
  private ObjectStore<Long> objectStore = mock(ObjectStore.class);
  private final FlowConstruct flowConstruct = mock(FlowConstruct.class);
  private MuleContext muleContext = mockContextWithServices();
  private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
  private EventGroup eventGroup = mock(EventGroup.class);
  private int countOfEventGroups = 0;
  private boolean eventGroupWasSaved = false;

  @Before
  public void setUp() throws Exception {
    setReturnsAndExceptions();
    setAnswers();
    eventCorrelator = new EventCorrelator(eventCorrelatorCallback, messageProcessor,
                                          muleContext, flowConstruct, partitionableObjectStore, "prefix", objectStore);
  }

  @Test
  public void testEventGroupFreedInRoutingException() throws Exception {
    CoreEvent event = mock(CoreEvent.class);
    try {
      eventCorrelator.process(event);
      fail("Routing Exception must be catched.");
    } catch (RoutingException e) {
      assertTrue("Event Group wasn't saved", eventGroupWasSaved);
      assertThat(countOfEventGroups, is(0));
    }

  }

  private void setReturnsAndExceptions() throws Exception {
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(partitionableObjectStore.retrieve(any(String.class), any(String.class)))
        .thenThrow(ObjectDoesNotExistException.class);
    when(eventCorrelatorCallback.createEventGroup(any(CoreEvent.class), any(Object.class))).thenReturn(eventGroup);
    when(eventCorrelatorCallback.aggregateEvents(any(EventGroup.class))).thenThrow(RoutingException.class);
    when(eventCorrelatorCallback.shouldAggregateEvents(any(EventGroup.class))).thenReturn(true);
  }

  private void setAnswers() throws ObjectStoreException {
    doAnswer(invocation -> {
      countOfEventGroups++;
      eventGroupWasSaved = true;
      return null;
    }).when(partitionableObjectStore).store(any(), any(), any());

    doAnswer(invocation -> {
      countOfEventGroups--;
      return null;
    }).when(partitionableObjectStore).remove(any(String.class), any(String.class));
  }
}


