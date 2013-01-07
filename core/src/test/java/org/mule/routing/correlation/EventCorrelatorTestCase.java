/*
 * $Id: EventCorrelator.java 24963 2012-10-22 21:55:05Z svacas $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.DefaultMessageCollection;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.routing.EventGroup;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class EventCorrelatorTestCase
{


    public static final String OBJECT_STOR_NAME_PREFIX = "prefix";
    public static final String TEST_GROUP_ID = "groupId";
    public static final boolean USE_PERSISTENT_STORE = false;


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
    private ListableObjectStore mockObjectStore;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventGroup mockEventGroup;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ListableObjectStore mockExpireGroupsObjectStore;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ListableObjectStore mockProcessedGroups;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DefaultMessageCollection mockMessageCollection;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleEvent mockMuleEvent;

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
        doThrow(new ObjectAlreadyExistsException()).when(mockObjectStore).store(TEST_GROUP_ID, mockEventGroup);
        EventCorrelator eventCorrelator = createEventCorrelator();
        eventCorrelator.addEventGroup(mockEventGroup);
        verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
    }

    @Test
    public void initAfterDeserializationAfterProcess() throws Exception
    {
        when(mockMessagingInfoMapping.getCorrelationId(isA(MuleMessage.class))).thenReturn(TEST_GROUP_ID);
        when(mockProcessedGroups.contains(TEST_GROUP_ID)).thenReturn(false);
        when(mockEventCorrelatorCallback.shouldAggregateEvents(mockEventGroup)).thenReturn(false);
        doThrow(new ObjectAlreadyExistsException()).when(mockObjectStore).store(TEST_GROUP_ID, mockEventGroup);
        EventCorrelator eventCorrelator = createEventCorrelator();
        eventCorrelator.process(mockMuleEvent);
        verify(mockEventGroup, times(1)).initAfterDeserialisation(mockMuleContext);
    }

    @Test
    public void disposeObjectStoresIfDisposable() throws Exception
    {
        mockExpireGroupsObjectStore = mock(DisposableListableObjectStore.class,RETURNS_DEEP_STUBS);
        mockProcessedGroups = mock(DisposableListableObjectStore.class, RETURNS_DEEP_STUBS);
        EventCorrelator eventCorrelator = createEventCorrelator();
        eventCorrelator.dispose();
        verify((Disposable)mockExpireGroupsObjectStore,times(1)).dispose();
        verify((Disposable)mockProcessedGroups,times(1)).dispose();
    }

    private EventCorrelator createEventCorrelator() throws Exception
    {
        when(mockMuleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER)).thenReturn(mockObjectStoreManager);
        when(mockObjectStoreManager.getObjectStore(OBJECT_STOR_NAME_PREFIX + ".eventGroups", USE_PERSISTENT_STORE)).thenReturn(mockObjectStore);
        when(mockObjectStoreManager.getObjectStore(OBJECT_STOR_NAME_PREFIX + ".expiredAndDispatchedGroups", USE_PERSISTENT_STORE)).thenReturn(mockExpireGroupsObjectStore);
        when(mockObjectStoreManager.getObjectStore(OBJECT_STOR_NAME_PREFIX + ".processedGroups", USE_PERSISTENT_STORE, EventCorrelator.MAX_PROCESSED_GROUPS, -1, 1000)).thenReturn(mockProcessedGroups);
        when(mockObjectStore.retrieve(TEST_GROUP_ID)).thenReturn(mockEventGroup);
        when(mockEventGroup.getGroupId()).thenReturn(TEST_GROUP_ID);
        when(mockEventGroup.toMessageCollection()).thenReturn(null);
        return new EventCorrelator(mockEventCorrelatorCallback, mockTimeoutMessageProcessor, mockMessagingInfoMapping, mockMuleContext, "flowName", USE_PERSISTENT_STORE, OBJECT_STOR_NAME_PREFIX);
    }

    public interface DisposableListableObjectStore extends ListableObjectStore, Disposable
    {
    }
}
