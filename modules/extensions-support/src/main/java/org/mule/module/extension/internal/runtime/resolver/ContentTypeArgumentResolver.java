/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.mule.module.extension.internal.ExtensionProperties.CONTENT_TYPE;
import org.mule.extension.api.runtime.ContentType;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

/**
 * Returns the value of the {@link ExtensionProperties#CONTENT_TYPE} variable,
 * which is expected to have been previously set on the supplied {@link OperationContext}.
 * <p>
 * Notice that for this to work, the {@link OperationContext}
 * has to be an instance of {@link OperationContextAdapter}
 *
 * @since 4.0
 */
public final class ContentTypeArgumentResolver implements ArgumentResolver<ContentType>
{

    /**
     * @param operationContext an {@link OperationContext}
     * @return the value of {@link OperationContextAdapter#getVariable(String)} using the {@link ExtensionProperties#CONTENT_TYPE}
     * @throws ClassCastException       if {@code operationContext} is not an instance of {@link OperationContextAdapter}
     * @throws IllegalArgumentException if the variable has not been set on the {@code operationContext}
     */
    @Override
    public ContentType resolve(OperationContext operationContext)
    {
        ContentType contentType = ((OperationContextAdapter) operationContext).getVariable(CONTENT_TYPE);
        if (contentType == null)
        {
            throw new IllegalArgumentException("ContentType was not set on the provided operationContext. " +
                                               "This is likely due to an offending interceptor or platform bug");
        }

        return contentType;
    }
}
