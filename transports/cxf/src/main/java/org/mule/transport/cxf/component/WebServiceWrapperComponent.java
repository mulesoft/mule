/*
 * $Id: WebServiceWrapperComponent.java 12228 2008-07-03 00:26:59Z aguenther $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.component;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.soap.component.AbstractWebServiceWrapperComponent;

public class WebServiceWrapperComponent extends AbstractWebServiceWrapperComponent
{
    private String wsdlPort;
    private String operation;

    protected MuleMessage doInvoke(MuleEvent event) throws Exception
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

        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("cxf:" + tempUrl, muleContext);
        if (wsdlPort != null)
        {
            endpointBuilder.setProperty("wsdlPort", wsdlPort);
        }
        if (operation != null)
        {
            endpointBuilder.setProperty("operation", operation);

        }

        OutboundEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            endpointBuilder);

        MuleMessage result = event.getSession().sendEvent(message, endpoint);
        return result;
    }

    public String getWsdlPort()
    {
        return wsdlPort;
    }

    public void setWsdlPort(String wsdlPort)
    {
        this.wsdlPort = wsdlPort;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

}
