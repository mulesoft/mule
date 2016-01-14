/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.metadata.DataType;
import org.mule.extension.api.runtime.ContentType;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

/**
 * Base class for {@link ArgumentResolver} implementations which depend
 * on metadata from the current {@link MuleEvent} or {@link MuleMessage}
 *
 * @param <T> the type to be returned by {@link #resolve(OperationContext)}
 * @since 4.0
 */
abstract class AbstractMetadataArgumentResolver<T> implements ArgumentResolver<T>
{

    /**
     * Returns a {@link ContentType} object representing the {@link DataType}
     * of the {@link MuleMessage} on which the operation is being executed
     *
     * @param operationContext the current {@link OperationContext}
     * @return a {@link ContentType}
     */
    protected ContentType getCurrentContentType(OperationContext operationContext)
    {
        DataType messageDataType = ((OperationContextAdapter) operationContext).getEvent().getMessage().getDataType();
        return new ContentType(messageDataType.getEncoding(), messageDataType.getMimeType());
    }
}
