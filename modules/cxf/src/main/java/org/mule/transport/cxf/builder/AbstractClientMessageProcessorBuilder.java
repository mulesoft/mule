package org.mule.transport.cxf.builder;

import org.mule.transport.cxf.support.StreamClosingInterceptor;

import org.apache.cxf.endpoint.Client;

/**
 * An abstract builder for non proxy clients.
 */
public abstract class AbstractClientMessageProcessorBuilder extends AbstractOutboundMessageProcessorBuilder
{
    protected Class serviceClass;
    
    protected void configureClient(Client client)
    {
        // EE-1806/MULE-4404
        client.getInInterceptors().add(new StreamClosingInterceptor());
        client.getInFaultInterceptors().add(new StreamClosingInterceptor());
    }
    
    public void setServiceClass(Class serviceClass)
    {
        this.serviceClass = serviceClass;
    }

    public Class getServiceClass()
    {
        return serviceClass;
    }

}
