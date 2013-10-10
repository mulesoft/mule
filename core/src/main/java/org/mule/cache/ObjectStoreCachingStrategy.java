/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cache;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.cache.filter.ConsumableMuleMessageFilter;
import org.mule.cache.keygenerator.KeyGenerator;
import org.mule.cache.keygenerator.MD5KeyGenerator;
import org.mule.cache.responsegenerator.DefaultResponseGenerator;
import org.mule.cache.responsegenerator.ResponseGenerator;
import org.mule.util.store.InMemoryObjectStore;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements {@link CachingStrategy} using an {@link ObjectStore} as a cache.
 * <p/>
 * Object's keys are generated using a {@link KeyGenerator} and the responses
 * are generated using a {@link ResponseGenerator}.
 * The caching strategy will only cache the {@link MuleEvent} that have a
 * non consumable message's payload. This check is done in both request and response
 * events using a configurable {@link Filter}.
 */
public class ObjectStoreCachingStrategy implements CachingStrategy
{

    protected Log logger = LogFactory.getLog(getClass());

    private ObjectStore<MuleEvent> store = new InMemoryObjectStore<MuleEvent>();

    private KeyGenerator keyGenerator = new MD5KeyGenerator();

    private ResponseGenerator responseGenerator = new DefaultResponseGenerator();

    private Filter consumableFilter = new ConsumableMuleMessageFilter();

    private String name;

    public MuleEvent process(MuleEvent request, MessageProcessor messageProcessor) throws MuleException
    {
        if (consumableFilter.accept(request.getMessage()))
        {
            Serializable key;
            try
            {
                key = keyGenerator.generateKey(request);
            }
            catch (Exception e)
            {
                logger.warn("Message will be processed without cache: key generation error", e);
                return messageProcessor.process(request);
            }

            return processMessageWithCache(key, request, messageProcessor);
        }
        else
        {
            return messageProcessor.process(request);
        }
    }

    private MuleEvent processMessageWithCache(Serializable key, MuleEvent request, MessageProcessor messageProcessor) throws MuleException
    {
        MuleEvent cachedResponse = lookupEventInCache(key);

        MuleEvent response;

        if (cachedResponse != null)
        {
            response = responseGenerator.create(request, cachedResponse);
        }
        else
        {
            response = messageProcessor.process(request);

            if (response == null || consumableFilter.accept(response.getMessage()))
            {
                MuleEvent responseCopy = response;
                if (response instanceof ThreadSafeAccess)
                {
                    responseCopy = (MuleEvent) ((ThreadSafeAccess) response).newThreadCopy();
                }
                store(key, responseCopy);
            }
        }

        return response;
    }

    private MuleEvent lookupEventInCache(Serializable key)
    {
        MuleEvent event = retrieve(key);

        if (logger.isDebugEnabled())
        {
            if (event != null)
            {
                logger.debug("Cache hit for key: " + key + " Event: " + event);
            }
            else
            {
                logger.debug("Cache miss for key: " + key);
            }
        }

        return event;
    }

    protected void store(Serializable key, MuleEvent value)
    {
        try
        {
            store.store(key, value);
        }
        catch (ObjectAlreadyExistsException e)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("An object with the specified key already exists in the object store (" + key + ")");
            }
        }
        catch (ObjectStoreException e)
        {
            // Logs a warning to indicate that there is an error accessing the
            // object store, but does not re-throw the exception to avoid
            // affecting the current event being processed, which can continue with
            // out caching.
            logger.warn("Unable to store event in cache", e);
        }
    }

    protected MuleEvent retrieve(Serializable key)
    {
        try
        {
            return store.retrieve(key);
        }
        catch (ObjectDoesNotExistException e)
        {
            // Nothing to do
        }
        catch (ObjectStoreException e)
        {
            // Logs a warning to indicate that there is an error accessing the
            // object store, but does not re-throw the exception to avoid
            // affecting the current event being processed, which can continue with
            // out caching.
            logger.warn("Unable to retrieve object from cache", e);
        }

        return null;
    }

    public ObjectStore<MuleEvent> getStore()
    {
        return store;
    }

    public void setStore(ObjectStore<MuleEvent> store)
    {
        this.store = store;
    }

    public KeyGenerator getKeyGenerator()
    {
        return keyGenerator;
    }

    public void setKeyGenerator(KeyGenerator keyGenerator)
    {
        this.keyGenerator = keyGenerator;
    }

    public ResponseGenerator getResponseGenerator()
    {
        return responseGenerator;
    }

    public void setResponseGenerator(ResponseGenerator responseGenerator)
    {
        this.responseGenerator = responseGenerator;
    }

    public Filter getConsumableFilter()
    {
        return consumableFilter;
    }

    public void setConsumableFilter(Filter consumableFilter)
    {
        this.consumableFilter = consumableFilter;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
