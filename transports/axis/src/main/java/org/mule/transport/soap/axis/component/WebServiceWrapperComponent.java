/*
 * $Id: WebServiceWrapperComponent.java 12228 2008-07-03 00:26:59Z aguenther $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis.component;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.soap.axis.AxisConnector;
import org.mule.transport.soap.component.AbstractWebServiceWrapperComponent;

import java.util.Map;

public class WebServiceWrapperComponent extends AbstractWebServiceWrapperComponent
{

    private String use;
    private String style;
    private Map properties;

    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        MuleContext muleContext = event.getMuleContext();

        String tempUrl;
        if (addressFromMessage)
        {
            tempUrl = event.getMessage().getStringProperty(WS_SERVICE_URL, null);
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
        MuleMessage message = new DefaultMuleMessage(event.transformMessage(), muleContext);

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

        OutboundEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            endpointBuilder);

        MuleMessage result = event.getSession().sendEvent(message, endpoint);
        return result;
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

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

}
