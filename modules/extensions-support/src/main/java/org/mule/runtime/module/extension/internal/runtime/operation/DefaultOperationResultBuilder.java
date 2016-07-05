/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;

import java.io.Serializable;
import java.util.Optional;

/**
 * Default implementation of {@link OperationResult.Builder}
 *
 * @param <OUTPUT>     the generic type of the output value
 * @param <ATTRIBUTES> the generic type of the message attributes
 * @since 4.0
 */
final class DefaultOperationResultBuilder<OUTPUT, ATTRIBUTES extends Serializable> implements OperationResult.Builder<OUTPUT, ATTRIBUTES>
{

    private final DefaultOperationResult<OUTPUT, ATTRIBUTES> operationResult = new DefaultOperationResult<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult.Builder<OUTPUT, ATTRIBUTES> output(OUTPUT output)
    {
        operationResult.output = output;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult.Builder<OUTPUT, ATTRIBUTES> attributes(ATTRIBUTES attributes)
    {
        operationResult.attributes = ofNullable(attributes);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult.Builder<OUTPUT, ATTRIBUTES> mediaType(MediaType dataType)
    {
        operationResult.mediaType = ofNullable(dataType);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult<OUTPUT, ATTRIBUTES> build()
    {
        return operationResult;
    }

    private final class DefaultOperationResult<OUTPUT, ATTRIBUTES extends Serializable> implements OperationResult<OUTPUT, ATTRIBUTES>
    {

        private OUTPUT output;
        private Optional<ATTRIBUTES> attributes = empty();
        private Optional<MediaType> mediaType = empty();

        public OUTPUT getOutput()
        {
            return output;
        }

        public Optional<ATTRIBUTES> getAttributes()
        {
            return attributes;
        }

        @Override
        public Optional<MediaType> getMediaType()
        {
            return mediaType;
        }
    }
}
