/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.RoutingException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.routing.EventGroup;
import org.mule.routing.correlation.EventCorrelator;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;


public class EventCorrelatorTestCase extends AbstractMuleTestCase
{

    private EventCorrelator eventCorrelator;
    private final EventCorrelatorCallback eventCorrelatorCallback = mock(EventCorrelatorCallback.class);
    private final MessageProcessor messageProcessor = mock(MessageProcessor.class);
    private final MessageInfoMapping messageInfoMapping = mock(MessageInfoMapping.class);
    private final FlowConstruct flowConstruct = mock(FlowConstruct.class);
    private ObjectStoreManager objectStoreManager = mock(ObjectStoreManager.class);
    private ListableObjectStore<EventGroup> eventGroupsObjectStore = mock(ListableObjectStore.class);
    private ListableObjectStore proccesedGroupsObjectStore = mock(ListableObjectStore.class);
    private MuleContext muleContext = mock(MuleContext.class);
    private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    private EventGroup eventGroup = mock(EventGroup.class);
    private MuleRegistry muleRegistry = mock(MuleRegistry.class);
    private int countOfEventGroups = 0;
    private boolean eventGroupWasSaved = false;

    @Before
    public void setUp() throws Exception
    {
        setReturnsAndExceptions();
        setAnswers();
        eventCorrelator = new EventCorrelator(eventCorrelatorCallback, messageProcessor, messageInfoMapping,
                                              muleContext, flowConstruct, true, "prefix");
    }

    @Test
    public void testEventGroupFreedInRoutingException() throws Exception
    {
        MuleEvent event = mock(MuleEvent.class);
        try
        {
            eventCorrelator.process(event);
            fail("Routing Exception must be catched.");
        }
        catch (RoutingException e)
        {
            assertTrue("Event Group wasn't saved", eventGroupWasSaved);
            assertThat(countOfEventGroups, is(0));
        }
    }

    private void setReturnsAndExceptions() throws Exception
    {
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(muleRegistry.get(any(String.class))).thenReturn(objectStoreManager);
        when(objectStoreManager.getObjectStore(any(String.class), any(Boolean.class), any(Integer.class), any(Integer.class), any(Integer.class))).thenReturn(proccesedGroupsObjectStore);
        when(objectStoreManager.getObjectStore(any(String.class), any(Boolean.class))).thenReturn((ListableObjectStore) eventGroupsObjectStore);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
        when(messageInfoMapping.getCorrelationId(any(MuleMessage.class))).thenReturn("id");
        when(eventCorrelatorCallback.createEventGroup(any(MuleEvent.class), any(Object.class))).thenReturn(eventGroup);
        when(eventCorrelatorCallback.aggregateEvents(any(EventGroup.class))).thenThrow(RoutingException.class);
        when(eventCorrelatorCallback.shouldAggregateEvents(any(EventGroup.class))).thenReturn(true);
        when(proccesedGroupsObjectStore.retrieve(any(Serializable.class))).thenThrow(ObjectDoesNotExistException.class);
        when(eventGroupsObjectStore.retrieve(any(Serializable.class))).thenThrow(ObjectDoesNotExistException.class);
    }

    private void setAnswers() throws ObjectStoreException
    {
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                countOfEventGroups++;
                eventGroupWasSaved = true;
                return null;
            }
        }).when(eventGroupsObjectStore).store(any(Serializable.class), any(EventGroup.class));

        doAnswer(new Answer<Void>()
        {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                countOfEventGroups--;
                return null;
            }
        }).when(eventGroupsObjectStore).remove(any(Serializable.class));
    }
}


