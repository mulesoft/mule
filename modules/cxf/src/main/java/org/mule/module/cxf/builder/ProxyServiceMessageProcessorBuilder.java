/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import org.mule.module.cxf.CxfConstants;
import org.mule.module.cxf.support.CopyAttachmentInInterceptor;
import org.mule.module.cxf.support.CopyAttachmentOutInterceptor;
import org.mule.module.cxf.support.CxfUtils;
import org.mule.module.cxf.support.OutputPayloadInterceptor;
import org.mule.module.cxf.support.ProxyRPCInInterceptor;
import org.mule.module.cxf.support.ProxySchemaValidationInInterceptor;
import org.mule.module.cxf.support.ProxyService;
import org.mule.module.cxf.support.ProxyServiceFactoryBean;
import org.mule.module.cxf.support.ProxyWSDLQueryHandler;
import org.mule.module.cxf.support.ResetStaxInterceptor;
import org.mule.module.cxf.support.ReversibleStaxInInterceptor;
import org.mule.module.cxf.support.ReversibleValidatingInterceptor;

import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCInInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBindingFeature;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.BareOutInterceptor;
import org.apache.cxf.transports.http.QueryHandler;

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

    @Override
    protected ServerFactoryBean createServerFactory() throws Exception
    {
        ServerFactoryBean sfb = new ServerFactoryBean();
        sfb.setDataBinding(new StaxDataBinding());
        sfb.getFeatures().add(new StaxDataBindingFeature());

        ProxyServiceFactoryBean proxyServiceFactoryBean = new ProxyServiceFactoryBean();
        proxyServiceFactoryBean.setSoapVersion(getSoapVersion());
        sfb.setServiceFactory(proxyServiceFactoryBean);

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
    protected QueryHandler getWSDLQueryHandler()
    {
        return new ProxyWSDLQueryHandler(getConfiguration().getCxfBus(), getPort());
    }

    @Override
    protected void configureServer(Server server)
    {
        if (isProxyEnvelope())
        {
            CxfUtils.removeInterceptor(server.getEndpoint().getBinding().getOutInterceptors(), SoapOutInterceptor.class.getName());
        }
        CxfUtils.removeInterceptor(server.getEndpoint().getBinding().getInInterceptors(), MustUnderstandInterceptor.class.getName());

        replaceRPCInterceptors(server);

        if (isValidationEnabled())
        {
            server.getEndpoint().getInInterceptors().add(new ProxySchemaValidationInInterceptor(getConfiguration().getCxfBus(),
                    server.getEndpoint().getService().getServiceInfos().get(0)));
        }
    }

    /**
     * When the binding style is RPC we need to replace the default interceptors to avoid truncating the content.
     */
    private void replaceRPCInterceptors(Server server)
    {
        if(CxfUtils.removeInterceptor(server.getEndpoint().getBinding().getInInterceptors(), RPCInInterceptor.class.getName()))
        {
            server.getEndpoint().getBinding().getInInterceptors().add(new ProxyRPCInInterceptor());
        }

        if(CxfUtils.removeInterceptor(server.getEndpoint().getBinding().getOutInterceptors(), RPCOutInterceptor.class.getName()))
        {
            server.getEndpoint().getBinding().getOutInterceptors().add(new BareOutInterceptor());
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
        /* Even if the payload is body, if validation is enabled, then we need to use a ReversibleXMLStreamReader to
         * avoid the message from being consumed during schema validation.
         */
        else if(isValidationEnabled())
        {
            sfb.getInInterceptors().add(new ReversibleValidatingInterceptor());
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
