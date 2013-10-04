/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.component;

import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.cxf.endpoint.CxfEndpointBuilder;

@Deprecated
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

        EndpointBuilder endpointBuilder = new CxfEndpointBuilder("cxf:" + tempUrl, muleContext);
        if (wsdlPort != null)
        {
            endpointBuilder.setProperty("port", wsdlPort);
        }
        if (operation != null)
        {
            endpointBuilder.setProperty("operation", operation);

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
