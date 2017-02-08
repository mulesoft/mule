/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.routing.correlation;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class EventCorrelatorMemoryLeakTestCase extends AbstractMuleTestCase
{

    private EventCorrelator eventCorrelator;
    private final EventCorrelatorCallback eventCorrelatorCallback = mock(EventCorrelatorCallback.class);
    private final MessageProcessor messageProcessor = mock(MessageProcessor.class);
    private final MessageInfoMapping messageInfoMapping = mock(MessageInfoMapping.class);
    private final FlowConstruct flowConstruct = mock(FlowConstruct.class);
    private PartitionableObjectStore<EventGroup> partitionableObjectStore = mock(PartitionableObjectStore.class);
    private ObjectStore<Long> objectStore =  mock(ObjectStore.class);
    private MuleEvent event = mock(MuleEvent.class);
    private MuleContext muleContext = mock(MuleContext.class);
    private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    private EventGroup eventGroup = mock(EventGroup.class);
    private int countOfEventGroups = 0;
    private boolean eventGroupWasSaved = false;

    @Before
    public void setUp() throws Exception
    {
        setReturnsAndExceptions();
        setAnswers();
        eventCorrelator = new EventCorrelator(eventCorrelatorCallback, messageProcessor, messageInfoMapping,
                                              muleContext, flowConstruct, partitionableObjectStore, "prefix", objectStore);
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
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
        when(messageInfoMapping.getCorrelationId(any(MuleMessage.class))).thenReturn("id");
        when(partitionableObjectStore.retrieve(any(Serializable.class), any(String.class))).thenThrow(ObjectDoesNotExistException.class);
        when(eventCorrelatorCallback.createEventGroup(any(MuleEvent.class), any(Object.class))).thenReturn(eventGroup);
        when(eventCorrelatorCallback.aggregateEvents(any(EventGroup.class))).thenThrow(RoutingException.class);
        when(eventCorrelatorCallback.shouldAggregateEvents(any(EventGroup.class))).thenReturn(true);
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
        }).when(partitionableObjectStore).store(any(Serializable.class), any(EventGroup.class), any(String.class));

        doAnswer(new Answer<Void>()
        {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                countOfEventGroups--;
                return null;
            }
        }).when(partitionableObjectStore).remove(any(Serializable.class), any(String.class));
    }
}


