/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.component;

import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.module.cxf.component.AbstractWebServiceWrapperComponent;
import org.mule.transport.soap.axis.AxisConnector;

import java.util.Map;

public class WebServiceWrapperComponent extends AbstractWebServiceWrapperComponent
{
    private String use;
    private String style;
    private Map<String, Object> properties;

    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        String tempUrl;
        if (addressFromMessage)
        {
            tempUrl = event.getMessage().getInboundProperty(WS_SERVICE_URL);
            if (tempUrl == null)
            {
                throw new IllegalArgumentException(CoreMessages.propertyIsNotSetOnEvent(WS_SERVICE_URL)
                    .toString());
            }
        }
        else
        {
            tempUrl = address;
        }

        MuleMessage message = event.getMessage();
        if (properties != null && properties.get(AxisConnector.SOAP_METHODS) != null)
        {
            message.addProperties((Map) properties.get(AxisConnector.SOAP_METHODS));
        }

        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("axis:" + tempUrl, muleContext);
        if (use != null)
        {
            endpointBuilder.setProperty(AxisConnector.USE, use);
        }
        if (style != null)
        {
            endpointBuilder.setProperty(AxisConnector.STYLE, style);

        }
        //TODO MULE-4952 what is the strategy here for proxy components?
        endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);

        OutboundEndpoint endpoint = endpointBuilder.buildOutboundEndpoint();

        MuleEvent responseEvent = endpoint.process(event);

        if (responseEvent != null && !VoidMuleEvent.getInstance().equals(responseEvent))
        {
            return responseEvent.getMessage();
        }
        else
        {
            return null;
        }
    }

    public String getUse()
    {
        return use;
    }

    public void setUse(String use)
    {
        this.use = use;
    }

    public String getStyle()
    {
        return style;
    }

    public void setStyle(String style)
    {
        this.style = style;
    }

    public Map<String, Object> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }
}
