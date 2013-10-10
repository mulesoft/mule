/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.builder;

import org.mule.module.cxf.support.StreamClosingInterceptor;

import org.apache.cxf.endpoint.Client;

/**
 * An abstract builder for non proxy clients.
 */
public abstract class AbstractClientMessageProcessorBuilder extends AbstractOutboundMessageProcessorBuilder
{
    protected Class<?> serviceClass;
    
    @Override
    protected void configureClient(Client client)
    {
        // EE-1806/MULE-4404
        client.getInInterceptors().add(new StreamClosingInterceptor());
        client.getInFaultInterceptors().add(new StreamClosingInterceptor());
    }
    
    public void setServiceClass(Class<?> serviceClass)
    {
        this.serviceClass = serviceClass;
    }

    public Class<?> getServiceClass()
    {
        return serviceClass;
    }
}
