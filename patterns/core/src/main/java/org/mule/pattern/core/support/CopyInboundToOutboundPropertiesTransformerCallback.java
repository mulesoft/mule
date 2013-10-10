/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.pattern.core.support;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.transformer.TransformerTemplate.TransformerCallback;
import org.mule.util.StringUtils;

public final class CopyInboundToOutboundPropertiesTransformerCallback implements TransformerCallback
{
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
