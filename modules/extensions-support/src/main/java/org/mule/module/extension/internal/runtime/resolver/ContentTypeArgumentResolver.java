/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.extension.api.runtime.ContentType;
import org.mule.extension.api.runtime.OperationContext;

/**
 * Resolves instances of {@link ContentType} which represent the {@link DataType}
 * of the {@link MuleMessage} on which the operation is being executed
 *
 * @since 4.0
 */
public class ContentTypeArgumentResolver extends AbstractMetadataArgumentResolver<ContentType>
{

    /**
     * Returns a {@link ContentType} which represents the {@link DataType}
     * of the {@link MuleMessage} on which the operation is being executed
     *
     * @param operationContext the current {@link OperationContext}
     * @return a {@link ContentType}
     */
    @Override
    public ContentType resolve(OperationContext operationContext)
    {
        return getCurrentContentType(operationContext);
    }
}
