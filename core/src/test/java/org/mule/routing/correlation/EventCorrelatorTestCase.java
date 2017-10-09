/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.DefaultMessageCollection;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.store.PartitionableObjectStore;
import org.mule.routing.EventGroup;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.size.SmallTest;
import org.mule.util.store.PartitionedInMemoryObjectStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class EventCorrelatorTestCase extends AbstractMuleTestCase
{

    public static final String OBJECT_STOR_NAME_PREFIX = "prefix";
    public static final String TEST_GROUP_ID = "groupId";
    public static final boolean USE_PERSISTENT_STORE = false;

    private static final Logger LOGGER = getLogger(EventCorrelatorTestCase.class);

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventCorrelatorCallback mockEventCorrelatorCallback;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessor mockTimeoutMessageProcessor;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageInfoMapping mockMessagingInfoMapping;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext mockMuleContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ObjectStoreManager mockObjectStoreManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventGroup mockEventGroup;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ListableObjectStore mockProcessedGroups;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DefaultMessageCollection mockMessageCollection;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FlowConstruct mockFlowConstruct;

    private PartitionableObjectStore memoryObjectStore = new PartitionedInMemoryObjectStore();

    @Test(expected = CorrelationTimeoutException.class)
    public void initAfterDeserializationAfterForceGroupExpiry() throws Exception
    {
        try
        {
            EventCorrelator eventCorrelator = createEventCorrelator();
            eventCorrelator.forceGroupExpiry(TEST_GROUP_ID);
        }
        finally
        {
            verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
        }
    }

    @Test
    public void initAfterDeserializationAfterAddEventGroup() throws Exception
    {
        EventCorrelator eventCorrelator = createEventCorrelator();
        eventCorrelator.addEventGroup(mockEventGroup);
        verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
    }

    @Test
    public void initAfterDeserializationAfterProcess() throws Exception
    {
        when(mockMessagingInfoMapping.getCorrelationId(isA(MuleMessage.class))).thenReturn(TEST_GROUP_ID);
        when(mockEventCorrelatorCallback.shouldAggregateEvents(mockEventGroup)).thenReturn(false);
        EventCorrelator eventCorrelator = createEventCorrelator();
        eventCorrelator.process(mockMuleEvent);
        verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
    }

    @Test
    public void processesExpiredGroupInPrimaryNode() throws Exception
    {
        doExpiredGroupMonitoringTest(true);
    }

    @Test
    public void doesNotProcessExpiredGroupInSecondaryNode() throws Exception
    {
        try
        {
            doExpiredGroupMonitoringTest(false);

            fail("Expiring group monitoring thread is not supposed to do any work on a secondary node");
        }
        catch (AssertionError e)
        {
            // Expected
        }
    }

    private void doExpiredGroupMonitoringTest(boolean primaryNode) throws Exception
    {
        when(mockMuleContext.isPrimaryPollingInstance()).thenReturn(primaryNode);

        EventCorrelator eventCorrelator = createEventCorrelator();
        when(mockEventCorrelatorCallback.createEventGroup(mockMuleEvent, TEST_GROUP_ID)).thenReturn(mockEventGroup);

        eventCorrelator.start();

        try
        {
            Prober prober = new PollingProber(1000, 50);
            prober.check(new Probe() {
                @Override
                public boolean isSatisfied()
                {
                    try
                    {
                        return !memoryObjectStore.contains(TEST_GROUP_ID, "prefix.eventGroups");
                    }
                    catch (ObjectStoreException e)
                    {
                        LOGGER.debug("Could not access object store.");
                        return false;
                    }
                }

                @Override
                public String describeFailure()
                {
                    return "Event group not expired.";
                }
            });
        }
        finally
        {
            eventCorrelator.stop();
            eventCorrelator.dispose();
        }
    }

    @Test
    public void avoidCreateMessageEventToGetExceptionListener() throws Exception
    {
        doExpiredGroupMonitoringTest(true);

        Mockito.verify(mockFlowConstruct, Mockito.times(1)).getExceptionListener();
        Mockito.verify(mockEventGroup, Mockito.times(1)).getMessageCollectionEvent();
    }


    /**
     * This test just verifies that in case a non expired event group is prompted
     * to expire, and no events are left to process (so it gets a null event from
     * the event to aggregate), the group expiry is handled without raising
     * an exception.
     * 
     * @throws Exception exception in case it fails.
     */
    @Test
    public void handleGroupExpiryWhenThereAreNoEventsToProcess() throws Exception
    {
        when(mockProcessedGroups.contains(1L)).thenReturn(true);
        when(mockEventGroup.getGroupId()).thenReturn(1L);
        when(mockEventGroup.getCreated()).thenReturn(currentTimeMillis());
        when(mockEventCorrelatorCallback.aggregateEvents(mockEventGroup)).thenReturn(null);
        EventCorrelator eventCorrelator = new EventCorrelator(mockEventCorrelatorCallback, mockTimeoutMessageProcessor, mockMessagingInfoMapping, mockMuleContext, mockFlowConstruct, memoryObjectStore,
                "prefix", mockProcessedGroups);
        eventCorrelator.setFailOnTimeout(false);
        eventCorrelator.handleGroupExpiry(mockEventGroup);
    }
    
    private EventCorrelator createEventCorrelator() throws Exception
    {
        when(mockMuleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).thenReturn(mockObjectStoreManager);
        memoryObjectStore.store(TEST_GROUP_ID, mockEventGroup, "prefix.eventGroups");
        when(mockEventGroup.getGroupId()).thenReturn(TEST_GROUP_ID);
        when(mockEventGroup.toMessageCollection()).thenReturn(null);
        when(mockFlowConstruct.getName()).thenReturn("flowName");
        return new EventCorrelator(mockEventCorrelatorCallback, mockTimeoutMessageProcessor, mockMessagingInfoMapping, mockMuleContext, mockFlowConstruct, memoryObjectStore,
                "prefix", mockProcessedGroups);
    }

    public interface DisposableListableObjectStore extends ListableObjectStore, Disposable
    {

    }
}
