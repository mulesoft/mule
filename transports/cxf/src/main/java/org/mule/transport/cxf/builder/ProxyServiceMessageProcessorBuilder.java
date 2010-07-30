package org.mule.transport.cxf.builder;

import org.mule.transport.cxf.CxfConstants;
import org.mule.transport.cxf.support.CopyAttachmentInInterceptor;
import org.mule.transport.cxf.support.CopyAttachmentOutInterceptor;
import org.mule.transport.cxf.support.CxfUtils;
import org.mule.transport.cxf.support.OutputPayloadInterceptor;
import org.mule.transport.cxf.support.ProxyService;
import org.mule.transport.cxf.support.ProxyServiceFactoryBean;
import org.mule.transport.cxf.support.ResetStaxInterceptor;
import org.mule.transport.cxf.support.ReversibleStaxInInterceptor;

import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBindingFeature;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;

/**
 * Creates an inbound proxy based on a specially configure CXF Server.
 * This allows you to send raw XML to your MessageProcessor and have it sent
 * through CXF for SOAP processing, WS-Security, etc.
 * <p>
 * The input to the resulting MessageProcessor can be either a SOAP Body
 * or a SOAP Envelope depending on how the payload attribute is configured.
 * Valid values are "body" or "envelope". 
 */
public class ProxyServiceMessageProcessorBuilder extends AbstractInboundMessageProcessorBuilder
{
    private String payload;
    
    protected ServerFactoryBean createServerFactory() throws Exception
    {
        ServerFactoryBean sfb = new ServerFactoryBean();
        sfb.setDataBinding(new StaxDataBinding());
        sfb.getFeatures().add(new StaxDataBindingFeature());
        sfb.setServiceFactory(new ProxyServiceFactoryBean());
        sfb.setServiceClass(ProxyService.class);
        
        addProxyInterceptors(sfb);
        
        return sfb;
    }

    @Override
    protected Class<?> getServiceClass()
    {
        return ProxyService.class;
    }

    @Override
    protected void configureServer(Server server)
    {
        if (isProxyEnvelope()) 
        {
            CxfUtils.removeInterceptor(server.getEndpoint().getBinding().getOutInterceptors(), SoapOutInterceptor.class.getName());
        }
        
    }

    @Override
    public boolean isProxy()
    {
        return true;
    }

    protected void addProxyInterceptors(ServerFactoryBean sfb)
    {
        sfb.getOutInterceptors().add(new OutputPayloadInterceptor());
        sfb.getInInterceptors().add(new CopyAttachmentInInterceptor());
        sfb.getOutInterceptors().add(new CopyAttachmentOutInterceptor());
        
        if (isProxyEnvelope()) 
        {
            sfb.getInInterceptors().add(new ReversibleStaxInInterceptor());
            sfb.getInInterceptors().add(new ResetStaxInterceptor());
        }
    }

    public boolean isProxyEnvelope()
    {
        return CxfConstants.PAYLOAD_ENVELOPE.equals(payload);
    }

    public String getPayload()
    {
        return payload;
    }

    public void setPayload(String payload)
    {
        this.payload = payload;
    }
    
}
