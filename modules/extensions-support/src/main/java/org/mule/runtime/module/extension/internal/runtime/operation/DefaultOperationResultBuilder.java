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
 * @param <Output>     the generic type of the output value
 * @param <Attributes> the generic type of the message attributes
 * @since 4.0
 */
final class DefaultOperationResultBuilder<Output, Attributes extends Serializable> implements OperationResult.Builder<Output, Attributes>
{

    private final DefaultOperationResult<Output, Attributes> operationResult = new DefaultOperationResult<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult.Builder<Output, Attributes> output(Output output)
    {
        operationResult.output = output;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult.Builder<Output, Attributes> attributes(Attributes attributes)
    {
        operationResult.attributes = ofNullable(attributes);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult.Builder<Output, Attributes> mediaType(MediaType dataType)
    {
        operationResult.mediaType = ofNullable(dataType);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult<Output, Attributes> build()
    {
        return operationResult;
    }

    private final class DefaultOperationResult<Output, Attributes extends Serializable> implements OperationResult<Output, Attributes>
    {

        private Output output;
        private Optional<Attributes> attributes = empty();
        private Optional<MediaType> mediaType = empty();

        public Output getOutput()
        {
            return output;
        }

        public Optional<Attributes> getAttributes()
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
