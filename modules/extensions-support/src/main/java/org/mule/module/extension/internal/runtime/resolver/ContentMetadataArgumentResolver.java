/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.mule.module.extension.internal.ExtensionProperties.CONTENT_METADATA;
import static org.mule.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import org.mule.api.MuleMessage;
import org.mule.api.metadata.DataType;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.extension.api.runtime.ContentType;
import org.mule.extension.api.runtime.FixedContextMetadata;
import org.mule.extension.api.runtime.MutableContentMetadata;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

import org.apache.commons.lang.StringUtils;

/**
 * Returns the value of the {@link ExtensionProperties#CONTENT_METADATA} variable,
 * which is expected to have been previously set on the supplied {@link OperationContext}.
 * <p>
 * Notice that for this to work, the {@link OperationContext}
 * has to be an instance of {@link OperationContextAdapter}
 *
 * @since 4.0
 */
public final class ContentMetadataArgumentResolver extends AbstractMetadataArgumentResolver<ContentMetadata>
{

    /**
     * Creates a {@link ContentMetadata} based on the {@link DataType} from the current
     * {@link MuleMessage}.
     *
     * @param operationContext an {@link OperationContext}
     * @return the value of {@link OperationContextAdapter#getVariable(String)} using the {@link ExtensionProperties#CONTENT_METADATA}
     * @throws ClassCastException       if {@code operationContext} is not an instance of {@link OperationContextAdapter}
     * @throws IllegalArgumentException if the variable has not been set on the {@code operationContext}
     */
    @Override
    public ContentMetadata resolve(OperationContext operationContext)
    {
        final ContentType currentContentType = getCurrentContentType(operationContext);
        String encoding = operationContext.getTypeSafeParameter(ENCODING_PARAMETER_NAME, String.class);
        String mimeType = operationContext.getTypeSafeParameter(MIME_TYPE_PARAMETER_NAME, String.class);

        boolean contentTypeFixed = !StringUtils.isBlank(encoding) || !StringUtils.isBlank(mimeType);

        ContentMetadata contentMetadata = contentTypeFixed
                                          ? new FixedContextMetadata(currentContentType, new ContentType(encoding, mimeType))
                                          : new MutableContentMetadata(currentContentType);

        //TODO: Remove this cast when MULE-8946 is ready
        ((OperationContextAdapter) operationContext).setVariable(CONTENT_METADATA, contentMetadata);
        return contentMetadata;
    }
}
