/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import org.mule.api.MuleEvent;
import org.mule.api.util.Copiable;
import org.mule.routing.MessageSequence;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class EventToMessageSequenceSplittingStrategyTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

    private EventToMessageSequenceSplittingStrategy strategy = new EventToMessageSequenceSplittingStrategy();
    private Collection<String> testCollection = Arrays.asList("Apple", "Banana", "Kiwi");

    @Test
    public void copiableCollection()
    {
        Copiable<Collection<String>> collection = mock(Copiable.class, withSettings().extraInterfaces(Collection.class));
        when(collection.copy()).thenReturn(testCollection);
        when(event.getMessage().getPayload()).thenReturn(collection);

        assertCollectionSequence();
        verify(collection).copy();
    }

    @Test
    public void nonCopiableCollection()
    {
        when(event.getMessage().getPayload()).thenReturn(testCollection);
        assertCollectionSequence();
    }

    private void assertCollectionSequence()
    {
        MessageSequence<String> sequence = (MessageSequence<String>) strategy.split(event);
        Iterator<String> expectedIterator = testCollection.iterator();
        while (sequence.hasNext())
        {
            assertThat(sequence.next(), is(sameInstance(expectedIterator.next())));
        }
    }
}
