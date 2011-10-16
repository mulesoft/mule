/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.cache;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.store.ObjectStore;
import org.mule.cache.keygenerator.KeyGenerator;
import org.mule.cache.responsegenerator.ResponseGenerator;
import org.mule.routing.filters.AcceptAllFilter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObjectStoreCachingStrategyTestCase extends AbstractMuleTestCase
{

    private static final String OBJECT_KEY = "key";

    @Test
    public void testCacheMiss() throws Exception
    {
        MuleEvent mockEvent = mock(MuleEvent.class);
        final MuleEvent mockResponse = mock(MuleEvent.class);

        KeyGenerator keyGenerator = mock(KeyGenerator.class);
        when(keyGenerator.generateKey(mockEvent)).thenReturn(OBJECT_KEY);

        ResponseGenerator responseGenerator = mock(ResponseGenerator.class);
        when(responseGenerator.create(mockEvent, mockResponse)).thenReturn(mockResponse);

        ObjectStore<MuleEvent> objectStore = mock(ObjectStore.class);
        when(objectStore.retrieve(OBJECT_KEY)).thenReturn(null);

        Filter consumablePayloadFilter = mock(Filter.class);
        when(consumablePayloadFilter.accept(Mockito.<MuleMessage>any())).thenReturn(true);

        ObjectStoreCachingStrategy cachingStrategy = new ObjectStoreCachingStrategy();
        cachingStrategy.setResponseGenerator(responseGenerator);
        cachingStrategy.setKeyGenerator(keyGenerator);
        cachingStrategy.setStore(objectStore);
        cachingStrategy.setConsumableFilter(consumablePayloadFilter);

        MessageProcessor cachedMessageProcessor = mock(MessageProcessor.class);
        when(cachedMessageProcessor.process(mockEvent)).thenReturn(mockResponse);
        MuleEvent response = cachingStrategy.process(mockEvent, cachedMessageProcessor);

        assertSame(mockResponse, response);
        Mockito.verify(objectStore, Mockito.times(1)).store(OBJECT_KEY, mockResponse);
    }

    @Test
    public void testCacheHit() throws Exception
    {
        MuleEvent mockEvent = mock(MuleEvent.class);
        final MuleEvent mockResponse = mock(MuleEvent.class);

        KeyGenerator keyGenerator = mock(KeyGenerator.class);
        when(keyGenerator.generateKey(mockEvent)).thenReturn(OBJECT_KEY);

        ResponseGenerator responseGenerator = mock(ResponseGenerator.class);
        when(responseGenerator.create(mockEvent, mockResponse)).thenReturn(mockResponse);

        ObjectStore<MuleEvent> objectStore = mock(ObjectStore.class);
        when(objectStore.retrieve(OBJECT_KEY)).thenReturn(mockResponse);

        ObjectStoreCachingStrategy cachingStrategy = new ObjectStoreCachingStrategy();
        cachingStrategy.setKeyGenerator(keyGenerator);
        cachingStrategy.setStore(objectStore);
        cachingStrategy.setResponseGenerator(responseGenerator);
        cachingStrategy.setConsumableFilter(new AcceptAllFilter());

        MessageProcessor cachedMessageProcessor = mock(MessageProcessor.class);
        MuleEvent response = cachingStrategy.process(mockEvent, cachedMessageProcessor);

        assertSame(mockResponse, response);
        Mockito.verify(objectStore, Mockito.times(0)).store(OBJECT_KEY, mockResponse);
        verify(cachedMessageProcessor, times(0)).process(mockEvent);
    }

    @Test
    public void testConsumableRequestPayloadIsNotCached() throws Exception
    {
        MuleEvent mockEvent = mock(MuleEvent.class);
        final MuleEvent mockResponse = mock(MuleEvent.class);

        ObjectStore<MuleEvent> objectStore = mock(ObjectStore.class);

        Filter consumablePayloadFilter = mock(Filter.class);
        when(consumablePayloadFilter.accept(Mockito.<MuleMessage>any())).thenReturn(false);

        ObjectStoreCachingStrategy cachingStrategy = new ObjectStoreCachingStrategy();
        cachingStrategy.setStore(objectStore);
        cachingStrategy.setConsumableFilter(consumablePayloadFilter);

        MessageProcessor cachedMessageProcessor = mock(MessageProcessor.class);
        when(cachedMessageProcessor.process(mockEvent)).thenReturn(mockResponse);
        MuleEvent response = cachingStrategy.process(mockEvent, cachedMessageProcessor);

        assertSame(mockResponse, response);
        Mockito.verify(objectStore, Mockito.times(0)).retrieve(mockEvent);
    }

    @Test
    public void testConsumableResponsePayloadIsNotCached() throws Exception
    {
        MuleEvent mockEvent = mock(MuleEvent.class);
        final MuleEvent mockResponse = mock(MuleEvent.class);

        KeyGenerator keyGenerator = mock(KeyGenerator.class);
        when(keyGenerator.generateKey(mockEvent)).thenReturn(OBJECT_KEY);

        ResponseGenerator responseGenerator = mock(ResponseGenerator.class);
        when(responseGenerator.create(mockEvent, mockResponse)).thenReturn(mockResponse);

        ObjectStore<MuleEvent> objectStore = mock(ObjectStore.class);

        Filter consumablePayloadFilter = mock(Filter.class);
        when(consumablePayloadFilter.accept(Mockito.<MuleMessage>any())).thenReturn(true).thenReturn(false);

        ObjectStoreCachingStrategy cachingStrategy = new ObjectStoreCachingStrategy();
        cachingStrategy.setKeyGenerator(keyGenerator);
        cachingStrategy.setResponseGenerator(responseGenerator);
        cachingStrategy.setStore(objectStore);
        cachingStrategy.setConsumableFilter(consumablePayloadFilter);

        MessageProcessor cachedMessageProcessor = mock(MessageProcessor.class);
        when(cachedMessageProcessor.process(mockEvent)).thenReturn(mockResponse);
        MuleEvent response = cachingStrategy.process(mockEvent, cachedMessageProcessor);


        assertSame(mockResponse, response);
        Mockito.verify(objectStore, Mockito.times(0)).store(OBJECT_KEY, mockResponse);
    }
}
