/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.io.InputStream;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a file's content so that operations can be performed over it regardless of its type.
 * <p>
 * The wrapped {@link #content} is not allowed to be {@code null}
 *
 * @since 4.0
 */
public final class FileContentWrapper
{

    private static final Logger LOGGER = LoggerFactory.getLogger(FileContentWrapper.class);

    private final Object content;
    private final MuleEvent event;
    private final MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param content the content to be wrapped
     */
    public FileContentWrapper(Object content, MuleEvent event, MuleContext muleContext)
    {
        checkArgument(content != null, "content cannot be null");
        checkArgument(event != null, "event cannot be null");

        this.content = content;
        this.event = event;
        this.muleContext = muleContext;
    }

    /**
     * Accepts the given {@code visitor}
     *
     * @param visitor a {@link FileContentVisitor}
     * @throws IllegalArgumentException if the {@link #content} is of a type not accepted by the {@code visitor}
     * @throws Exception                if the visitation failed
     */
    public void accept(FileContentVisitor visitor) throws Exception
    {
        if (content instanceof String)
        {
            visitor.visit((String) content);
        }
        else if (content instanceof InputStream)
        {
            visitor.visit((InputStream) content);
        }
        else if (content instanceof OutputHandler)
        {
            visitor.visit((OutputHandler) content);
        }
        else if (content instanceof String[])
        {
            visitor.visit((String[]) content);
        }
        else if (content instanceof Iterable)
        {
            visitor.visit((Iterable) content);
        }
        else if (content instanceof Iterator)
        {
            visitor.visit((Iterator) content);
        }
        else if (content instanceof byte[])
        {
            visitor.visit((byte[]) content);
        }
        else if (byte.class.isAssignableFrom(content.getClass()))
        {
            visitor.visit((byte) content);
        }
        else if (content instanceof Byte)
        {
            visitor.visit((Byte) content);
        }
        else
        {
            attemptFallbackTransformation(visitor);
        }
    }

    private void attemptFallbackTransformation(FileContentVisitor visitor) throws Exception
    {
        final DataType sourceDataType = DataTypeFactory.create(content.getClass());
        DataType targetDataType = DataTypeFactory.create(InputStream.class);

        Object transformedValue = tryTransform(sourceDataType, targetDataType);
        if (transformedValue != null)
        {
            visitor.visit((InputStream) transformedValue);
        }
        else
        {
            targetDataType = DataTypeFactory.create(String.class);
            transformedValue = tryTransform(sourceDataType, targetDataType);
            if (transformedValue != null)
            {
                visitor.visit((String) transformedValue);
            }
            else
            {
                throw new IllegalArgumentException(String.format("Contents of type '%s' are not visitable and no suitable transformer could be found", content.getClass().getName()));
            }
        }
    }

    private Object tryTransform(DataType sourceDataType, DataType targetDataType)
    {
        Transformer transformer;
        try
        {
            transformer = muleContext.getRegistry().lookupTransformer(sourceDataType, targetDataType);
        }
        catch (TransformerException e)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format("Could not find transformer for content of type '%s' to '%s'", content.getClass().getName(), targetDataType.getType().getName()), e);
            }
            return null;
        }

        try
        {
            if (transformer instanceof MessageTransformer)
            {
                return ((MessageTransformer) transformer).transform(content, castEvent());
            }
            else
            {
                return transformer.transform(content);
            }
        }
        catch (Exception e)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format("Found exception trying to transform content of type '%s' to '%s'", content.getClass().getName(), targetDataType.getType().getName()), e);
            }
            return null;
        }
    }

    private org.mule.runtime.core.api.MuleEvent castEvent()
    {
        return (org.mule.runtime.core.api.MuleEvent) event;
    }
}
