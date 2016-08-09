/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.correlation;

import static java.util.Optional.of;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.store.PartitionableObjectStore;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.routing.EventGroup;
import org.mule.runtime.core.util.store.PartitionedInMemoryObjectStore;
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
import org.slf4j.LoggerFactory;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class EventCorrelatorTestCase extends AbstractMuleTestCase {

  public static final String OBJECT_STOR_NAME_PREFIX = "prefix";
  public static final String TEST_GROUP_ID = "groupId";
  public static final boolean USE_PERSISTENT_STORE = false;

  private static final Logger logger = LoggerFactory.getLogger(EventCorrelatorTestCase.class);

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private EventCorrelatorCallback mockEventCorrelatorCallback;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MessageProcessor mockTimeoutMessageProcessor;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext mockMuleContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ObjectStoreManager mockObjectStoreManager;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private EventGroup mockEventGroup;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ListableObjectStore mockProcessedGroups;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleMessage mockMessageCollection;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleEvent mockMuleEvent;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FlowConstruct mockFlowConstruct;

  private PartitionableObjectStore memoryObjectStore = new PartitionedInMemoryObjectStore();

  @Before
  public void setup() {
    when(mockEventGroup.getMessageCollectionEvent()).thenReturn(mockMuleEvent);
    when(mockMuleEvent.getMessage()).thenReturn(mockMessageCollection);
    when(mockMessageCollection.getDataType()).thenReturn(DataType.OBJECT);
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
    final Correlation correlation = mock(Correlation.class);
    when(correlation.getId()).thenReturn(of(TEST_GROUP_ID));
    when(mockMessageCollection.getCorrelation()).thenReturn(correlation);
    when(mockEventCorrelatorCallback.shouldAggregateEvents(mockEventGroup)).thenReturn(false);
    EventCorrelator eventCorrelator = createEventCorrelator();
    eventCorrelator.process(mockMuleEvent);
    verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
  }

  @Test
  @Ignore("MULE-7311")
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
            logger.debug("Could not access object store.");
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
      eventCorrelator.dispose();
    }
  }

  @Test
  public void avoidCreateMessageEventToGetExceptionListener() throws Exception {
    doExpiredGroupMonitoringTest(true);

    verify(mockFlowConstruct, times(1)).getExceptionListener();
    verify(mockEventGroup, times(1)).getMessageCollectionEvent();
  }

  private EventCorrelator createEventCorrelator() throws Exception {
    when(mockMuleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).thenReturn(mockObjectStoreManager);
    memoryObjectStore.store(TEST_GROUP_ID, mockEventGroup, "prefix.eventGroups");
    when(mockEventGroup.getGroupId()).thenReturn(TEST_GROUP_ID);
    when(mockEventGroup.getMessageCollectionEvent()).thenReturn(mock(MuleEvent.class));
    when(mockFlowConstruct.getName()).thenReturn("flowName");
    return new EventCorrelator(mockEventCorrelatorCallback, mockTimeoutMessageProcessor, mockMuleContext, mockFlowConstruct,
                               memoryObjectStore, "prefix", mockProcessedGroups);
  }

  public interface DisposableListableObjectStore extends ListableObjectStore, Disposable {

  }
}
