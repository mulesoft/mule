/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;


import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;

/**
 * Builds an outbound CXF MessageProcessor based on a WSDL using CXF's
 * {@link DynamicClientFactory}. The <code>wsdlLocation</code> attribute
 * is required. The port and service attributes can also be supplied to 
 * select the correct service and port in the WSDL.
 */
public class WsdlClientMessageProcessorBuilder extends AbstractOutboundMessageProcessorBuilder
{
    private final static Object CLIENT_CREATION_LOCK = new Object();
    
    private String service;
    private String port;
    
    public WsdlClientMessageProcessorBuilder()
    {
        super();
    }
    
    protected Client createClient() throws Exception
    {
        synchronized (CLIENT_CREATION_LOCK)
        {
            DynamicClientFactory cf = DynamicClientFactory.newInstance(getBus());
            return cf.createClient(getWsdlLocation(), 
               (service == null ? null : QName.valueOf(service)), 
               (getPort() == null ? null : QName.valueOf(getPort())));
        }
    }

    public String getService()
    {
        return service;
    }

    public void setService(String service)
    {
        this.service = service;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }

}
