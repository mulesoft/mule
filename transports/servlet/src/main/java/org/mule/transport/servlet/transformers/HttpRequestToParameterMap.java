/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.transformers;

import org.mule.api.MuleMessage;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.servlet.ServletConnector;

import java.util.Map;

/**
 * Returns a simple Map of the parameters sent with the HTTP Request.
 * If the same parameter is given more than once, only the first value for it will be in the Map.
 */
public class HttpRequestToParameterMap extends AbstractMessageTransformer
{
    public HttpRequestToParameterMap()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.create(Map.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding)
    {
        return message.getInboundProperty(ServletConnector.PARAMETER_MAP_PROPERTY_KEY);
    }
}
