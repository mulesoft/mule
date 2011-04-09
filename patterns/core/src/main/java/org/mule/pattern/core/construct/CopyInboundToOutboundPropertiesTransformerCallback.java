/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.pattern.core.construct;

import org.mule.api.MuleMessage;
import org.mule.transformer.TransformerTemplate.TransformerCallback;

public final class CopyInboundToOutboundPropertiesTransformerCallback implements TransformerCallback
{
    public Object doTransform(final MuleMessage message) throws Exception
    {
        for (final String inboundPropertyName : message.getInboundPropertyNames())
        {
            message.setOutboundProperty(inboundPropertyName, message.getInboundProperty(inboundPropertyName));
        }

        return message;
    }
}
