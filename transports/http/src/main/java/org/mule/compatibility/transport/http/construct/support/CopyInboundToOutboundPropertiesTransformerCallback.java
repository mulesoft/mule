/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.construct.support;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.transformer.TransformerTemplate.TransformerCallback;
import org.mule.runtime.core.util.StringUtils;

public final class CopyInboundToOutboundPropertiesTransformerCallback implements TransformerCallback
{
    @Override
    public Object doTransform(final MuleMessage message) throws Exception
    {
        for (final String inboundPropertyName : message.getInboundPropertyNames())
        {
            if (StringUtils.startsWith(inboundPropertyName, MuleProperties.PROPERTY_PREFIX))
            {
                continue;
            }

            message.setOutboundProperty(inboundPropertyName, message.getInboundProperty(inboundPropertyName));
        }

        return message;
    }
}
