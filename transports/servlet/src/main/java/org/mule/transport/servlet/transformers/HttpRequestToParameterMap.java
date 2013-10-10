/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
