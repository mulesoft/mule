/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.runtime.api.metadata.ComponentId;
import org.mule.runtime.api.metadata.MetadataAware;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataManager;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationInstanceNotification;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.NotificationException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

/**
 * Default implementation of the {@link MetadataManager}, which provides access to the Metadata of any Component in the
 * application, using it's {@link ComponentId}.
 * Requires the injection of the {@link MuleContext}, to be able to lookup the component inside the Mule App flows
 * using the given {@link ComponentId}
 *
 * @since 4.0
 */
public class MuleMetadataManager implements MetadataManager, Initialisable
{

    private static final String EXCEPTION_RESOLVING_COMPONENT_METADATA = "An exception occurred while resolving metadata for component '%s'";
    private static final String COMPONENT_NOT_METADATA_AWARE = "Component is not MetadataAware, no information available";
    private static final String EXCEPTION_RESOLVING_METADATA_KEYS = "An exception occurred while resolving Component MetadataKeys";
    private static final String SOURCE_NOT_FOUND = "Flow doesn't contain a message source";
    private static final String PROCESSOR_NOT_FOUND = "Processor doesn't exist in the given index [%s]";

    @Inject
    private MuleContext muleContext;

    private final LoadingCache<String, MetadataCache> caches;

    public MuleMetadataManager()
    {
        caches = CacheBuilder.newBuilder().build(
                new CacheLoader<String, MetadataCache>()
                {
                    @Override
                    public MetadataCache load(String id) throws Exception
                    {
                        return new DefaultMetadataCache();
                    }
                });
    }

    /**
     * Initialize this instance by registering a {@link CustomNotificationListener}
     *
     * @throws InitialisationException
     */
    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            muleContext.registerListener((CustomNotificationListener<ConfigurationInstanceNotification>) notification -> {
                try
                {
                    if (notification.getAction() == ConfigurationInstanceNotification.CONFIGURATION_STOPPED)
                    {
                        String name = ((ConfigurationInstanceNotification) notification).getConfigurationInstance().getName();
                        disposeCache(name);
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error while looking for the MetadataManager in the registry", e);
                }
            });
        }
        catch (NotificationException e)
        {
            throw new InitialisationException(createStaticMessage("Could not register ConfigurationInstanceListener"), e, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<List<MetadataKey>> getMetadataKeys(ComponentId componentId)
    {
        return exceptionHandledMetadataFetch(componentId, MetadataAware::getMetadataKeys, EXCEPTION_RESOLVING_METADATA_KEYS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<ComponentMetadataDescriptor> getMetadata(ComponentId componentId, MetadataKey key)
    {
        return exceptionHandledMetadataFetch(componentId, processor -> processor.getMetadata(key),
                                             String.format(EXCEPTION_RESOLVING_COMPONENT_METADATA, componentId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<ComponentMetadataDescriptor> getMetadata(ComponentId componentId)
    {
        return exceptionHandledMetadataFetch(componentId, MetadataAware::getMetadata,
                                             String.format(EXCEPTION_RESOLVING_COMPONENT_METADATA, componentId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeCache(String id)
    {
        caches.invalidate(id);
    }

    public MetadataCache getMetadataCache(String id)
    {
        try
        {
            return caches.get(id);
        }
        catch (ExecutionException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not get the cache with id:" + id), e);
        }
    }

    public Map<String, ? extends MetadataCache> getMetadataCaches()
    {
        return ImmutableMap.copyOf(caches.asMap());
    }

    private <T> MetadataResult<T> exceptionHandledMetadataFetch(ComponentId componentId, MetadataDelegate<T> metadataSupplier, String failureMessage)
    {
        try
        {
            return metadataSupplier.get(findMetadataAwareComponent(componentId));
        }
        catch (InvalidComponentIdException e)
        {
            return MetadataResult.failure(e);
        }
        catch (Exception e)
        {
            return MetadataResult.failure(null, failureMessage, e);
        }
    }

    private MetadataAware findMetadataAwareComponent(ComponentId componentId) throws InvalidComponentIdException
    {
        //FIXME MULE-9496 : Use flow paths to obtain Processors
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(componentId.getFlowName());
        if (flow == null)
        {
            throw new InvalidComponentIdException(createStaticMessage(String.format(PROCESSOR_NOT_FOUND, componentId.getComponentPath())));
        }
        try
        {
            if (!componentId.getComponentPath().equals("-1"))
            {
                try
                {
                    return ((MetadataAware) flow.getMessageProcessors().get(Integer.parseInt(componentId.getComponentPath())));
                }
                catch (IndexOutOfBoundsException | NumberFormatException e)
                {
                    throw new InvalidComponentIdException(createStaticMessage(String.format(PROCESSOR_NOT_FOUND, componentId.getComponentPath())), e);
                }
            }
            else
            {
                final MessageSource messageSource = flow.getMessageSource();
                if (messageSource == null)
                {
                    throw new InvalidComponentIdException(createStaticMessage(SOURCE_NOT_FOUND));
                }
                return (MetadataAware) messageSource;
            }
        }
        catch (ClassCastException e)
        {
            throw new InvalidComponentIdException(createStaticMessage(COMPONENT_NOT_METADATA_AWARE), e);
        }
    }

    private interface MetadataDelegate<T>
    {
        MetadataResult<T> get(MetadataAware processor) throws MetadataResolvingException;

    }
}
