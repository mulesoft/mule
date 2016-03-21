/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.metadata;

import org.mule.api.MuleContext;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.resolving.MetadataResult;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.Flow;

import java.util.List;

import javax.inject.Inject;

/**
 * Default implementation of the {@link MetadataManager}, which provides access to the Metadata of any Component in the
 * application, using it's {@link ComponentId}.
 * Requires the injection of the {@link MuleContext}, to be able to lookup the component inside the Mule App flows
 * using the given {@link ComponentId}
 *
 * @since 4.0
 */
public class MuleMetadataManager implements MetadataManager
{

    private static final String EXCEPTION_RESOLVING_OPERATION_METADATA = "An exception occurred while resolving Operation %s metadata";
    private static final String PROCESSOR_NOT_METADATA_AWARE = "Operation is not MetadataAware, no information available";
    private static final String EXCEPTION_RESOLVING_METADATA_KEYS = "An exception occurred while resolving Operation MetadataKeys";
    private static final String SOURCE_NOT_FOUND = "Flow doesn't contain a message source";
    private static final String PROCESSOR_NOT_FOUND = "Processor doesn't exist in the given index [%s]";

    @Inject
    private MuleContext muleContext;

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
    public MetadataResult<OperationMetadataDescriptor> getMetadata(ComponentId componentId, MetadataKey key)
    {
        return exceptionHandledMetadataFetch(componentId, processor -> processor.getMetadata(key),
                                             String.format(EXCEPTION_RESOLVING_OPERATION_METADATA, componentId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataResult<OperationMetadataDescriptor> getMetadata(ComponentId componentId)
    {
        return exceptionHandledMetadataFetch(componentId, MetadataAware::getMetadata,
                                             String.format(EXCEPTION_RESOLVING_OPERATION_METADATA, componentId));
    }

    private <T> MetadataResult<T> exceptionHandledMetadataFetch(ComponentId componentId, MetadataDelegate<T> metadataSupplier, String failureMessage)
    {
        try
        {
            return metadataSupplier.get(findMetadataAwareExecutable(componentId));
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

    private MetadataAware findMetadataAwareExecutable(ComponentId componentId) throws InvalidComponentIdException
    {
        //FIXME MULE-9496 : Use flow paths to obtain Processors
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(componentId.getFlowName());
        if (flow == null)
        {
            throw new InvalidComponentIdException(MessageFactory.createStaticMessage(String.format(PROCESSOR_NOT_FOUND, componentId.getComponentPath())));
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
                    throw new InvalidComponentIdException(MessageFactory.createStaticMessage(String.format(PROCESSOR_NOT_FOUND, componentId.getComponentPath())), e);
                }
            }
            else
            {
                final MessageSource messageSource = flow.getMessageSource();
                if (messageSource == null)
                {
                    throw new InvalidComponentIdException(MessageFactory.createStaticMessage(SOURCE_NOT_FOUND));
                }
                return (MetadataAware) messageSource;
            }
        }
        catch (ClassCastException e)
        {
            throw new InvalidComponentIdException(MessageFactory.createStaticMessage(PROCESSOR_NOT_METADATA_AWARE), e);
        }
    }

    private interface MetadataDelegate<T>
    {

        MetadataResult<T> get(MetadataAware processor) throws MetadataResolvingException;
    }

}
