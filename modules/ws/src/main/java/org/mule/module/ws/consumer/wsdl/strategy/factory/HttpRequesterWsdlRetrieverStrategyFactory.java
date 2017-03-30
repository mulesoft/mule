/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer.wsdl.strategy.factory;

import org.mule.api.MuleContext;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.ws.consumer.HttpRequesterWsdlRetrieverStrategy;
import org.mule.module.ws.consumer.WsdlRetrieverStrategy;
import org.mule.transport.ssl.api.TlsContextFactory;

import javax.wsdl.WSDLException;

/**
 * Factory to create HttpRequesterWsdlRetrieverStrategy
 */
public class HttpRequesterWsdlRetrieverStrategyFactory implements WSDLRetrieverStrategyFactory
{

    private TlsContextFactory tlsContextFactory;
    private ProxyConfig proxyConfig;
    private MuleContext context;

    public HttpRequesterWsdlRetrieverStrategyFactory(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, MuleContext context)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.context = context;
    }

    @Override
    public WsdlRetrieverStrategy createWSDLRetrieverStrategy() throws WSDLException
    {
        return new HttpRequesterWsdlRetrieverStrategy(tlsContextFactory, proxyConfig, context);
    }

}
