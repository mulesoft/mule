/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
