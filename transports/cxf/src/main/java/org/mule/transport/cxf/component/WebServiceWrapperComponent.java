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

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.PropertyScope;
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
            tempUrl = event.getMessage().getStringProperty(WS_SERVICE_URL, PropertyScope.INBOUND, null);
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

        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("cxf:" + tempUrl, muleContext);
        if (wsdlPort != null)
        {
            endpointBuilder.setProperty("wsdlPort", wsdlPort);
        }
        if (operation != null)
        {
            endpointBuilder.setProperty("operation", operation);

        }

        //TODO MULE-4952 what is the strategy here for proxy components?
        endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
        OutboundEndpoint endpoint = endpointBuilder.buildOutboundEndpoint();
        
        MuleEvent responseEvent = endpoint.process(new DefaultMuleEvent(event.getMessage(), endpoint,
            event.getSession()));

        if (responseEvent != null)
        {
            return responseEvent.getMessage();
        }
        else
        {
            return null;
        }
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
