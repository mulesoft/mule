/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.correlation;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.tck.util.MuleContextUtils.registerIntoMockContext;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.routing.EventGroup;
import org.mule.runtime.core.internal.store.PartitionedInMemoryObjectStore;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import io.qameta.allure.Issue;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class EventCorrelatorTestCase extends AbstractMuleTestCase {

  public static final String TEST_GROUP_ID = "groupId";

  private static final Logger LOGGER = getLogger(EventCorrelatorTestCase.class);

  private MuleContextWithRegistries mockMuleContext = mockContextWithServices();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private EventCorrelatorCallback mockEventCorrelatorCallback;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Processor mockTimeoutMessageProcessor;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ObjectStoreManager mockObjectStoreManager;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private EventGroup mockEventGroup;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ObjectStore mockProcessedGroups;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private InternalMessage mockMessageCollection;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private CoreEvent mockMuleEvent;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FlowConstruct mockFlowConstruct;

  private PartitionableObjectStore memoryObjectStore = new PartitionedInMemoryObjectStore();

  @Before
  public void setup() throws RegistrationException {
    when(mockEventGroup.getMessageCollectionEvent()).thenReturn(mockMuleEvent);
    when(mockMuleEvent.getMessage()).thenReturn(mockMessageCollection);
    when(mockMessageCollection.getPayload()).thenReturn(new TypedValue<>(null, OBJECT));
  }

  @Test(expected = CorrelationTimeoutException.class)
  public void initAfterDeserializationAfterForceGroupExpiry() throws Exception {
    try {
      EventCorrelator eventCorrelator = createEventCorrelator();
      eventCorrelator.forceGroupExpiry(TEST_GROUP_ID);
    } finally {
      verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
    }
  }

  @Test
  public void initAfterDeserializationAfterAddEventGroup() throws Exception {
    EventCorrelator eventCorrelator = createEventCorrelator();
    eventCorrelator.addEventGroup(mockEventGroup);
    verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
  }

  @Test
  public void initAfterDeserializationAfterProcess() throws Exception {
    when(mockEventCorrelatorCallback.shouldAggregateEvents(mockEventGroup)).thenReturn(false);
    when(mockMuleEvent.getCorrelationId()).thenReturn(TEST_GROUP_ID);
    EventCorrelator eventCorrelator = createEventCorrelator();
    eventCorrelator.process(mockMuleEvent);
    verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
  }

  @Test
  @Ignore("MULE-7311")
  @Issue("MULE-7311")
  public void processesExpiredGroupInPrimaryNode() throws Exception {
    doExpiredGroupMonitoringTest(true);
  }

  @Test
  public void doesNotProcessExpiredGroupInSecondaryNode() throws Exception {
    try {
      doExpiredGroupMonitoringTest(false);

      fail("Expiring group monitoring thread is not supposed to do any work on a secondary node");
    } catch (AssertionError e) {
      // Expected
    }
  }

  private void doExpiredGroupMonitoringTest(boolean primaryNode) throws Exception {
    when(mockMuleContext.isPrimaryPollingInstance()).thenReturn(primaryNode);

    EventCorrelator eventCorrelator = createEventCorrelator();
    when(mockEventCorrelatorCallback.createEventGroup(mockMuleEvent, TEST_GROUP_ID)).thenReturn(mockEventGroup);

    eventCorrelator.start();

    try {
      Prober prober = new PollingProber(1000, 50);
      prober.check(new Probe() {

        @Override
        public boolean isSatisfied() {
          try {
            return !memoryObjectStore.contains(TEST_GROUP_ID, "prefix.eventGroups");
          } catch (ObjectStoreException e) {
            LOGGER.debug("Could not access object store.");
            return false;
          }
        }

        @Override
        public String describeFailure() {
          return "Event group not expired.";
        }
      });
    } finally {
      eventCorrelator.stop();
    }
  }

  @Test
  public void avoidCreateMessageEventToGetExceptionListener() throws Exception {
    doExpiredGroupMonitoringTest(true);

    verify(mockEventGroup, times(1)).getMessageCollectionEvent();
  }

  private EventCorrelator createEventCorrelator() throws Exception {
    registerIntoMockContext(mockMuleContext, OBJECT_STORE_MANAGER, mockObjectStoreManager);
    memoryObjectStore.store(TEST_GROUP_ID, mockEventGroup, "prefix.eventGroups");
    when(mockEventGroup.getGroupId()).thenReturn(TEST_GROUP_ID);
    when(mockEventGroup.getMessageCollectionEvent()).thenReturn(mock(CoreEvent.class));
    when(mockFlowConstruct.getName()).thenReturn("flowName");
    return new EventCorrelator(mockEventCorrelatorCallback, mockTimeoutMessageProcessor, mockMuleContext, mockFlowConstruct,
                               memoryObjectStore, "prefix", mockProcessedGroups);
  }

  public abstract class DisposableObjectStore implements ObjectStore, Disposable {

  }
}
