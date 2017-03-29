/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLLocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * A custom locator to replicate the context of requester in every step 
 * where wsdl4j has to retrieve a resource (imported XSD, WSDL)
 */
public class MuleWSDLLocator implements WSDLLocator
{
    private static final Logger logger = LoggerFactory.getLogger(WSConsumer.class);

    private String baseURI;
    private String latestImportedURI;
    private boolean useConnectorToRetrieveWsdl;
    private HttpRequesterWsdlRetrieverStrategy httpRetrieverStrategy;
    private URLRetrieverStrategy urlRetrieverStrategy;
    private Collection<InputStream> streams = new ArrayList<InputStream>();
    
    public MuleWSDLLocator(MuleWSDLLocatorConfig config) throws MuleException
    {
        this.baseURI = config.getBaseURI();
        this.useConnectorToRetrieveWsdl = config.isUseConnectorToRetrieveWsdl();
        createURLRetrieverStrategy();
        if (useConnectorToRetrieveWsdl)
        {
            createHttpRequesterWsdlRetrieverStrategy(config.getTlsContextFactory(), config.getProxyConfig(), config.getContext());
        }
    }


    private void createURLRetrieverStrategy()
    {
        urlRetrieverStrategy = new URLRetrieverStrategy();

    }


    private void createHttpRequesterWsdlRetrieverStrategy(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, MuleContext context) throws MuleException
    {
        httpRetrieverStrategy = new HttpRequesterWsdlRetrieverStrategy(tlsContextFactory, proxyConfig, context);
        httpRetrieverStrategy.startHttpClient();
    }


    @Override
    public InputSource getBaseInputSource()
    {
        try
        {
            return getInputSource(baseURI);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputSource getImportInputSource(String parentLocation, String importLocation)
    {

        try
        {
            latestImportedURI = IOUtils.getResourceAsUrl(importLocation, getClass()).toString();

            return getInputSource(importLocation);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    private InputSource getInputSource(String url) throws WSDLException
    {
        boolean isHttpRequester = verifyIsHttpRequester(url);

        InputStream resultStream = null;

        if (useConnectorToRetrieveWsdl && isHttpRequester)
        {
            resultStream = httpRetrieverStrategy.retrieveWsdlResource(url);
        }
        else
        {
            resultStream = urlRetrieverStrategy.retrieveWsdlResource(url);
        }

        streams.add(resultStream);

        return new InputSource(resultStream);
    }

    @Override
    public String getBaseURI()
    {
        return baseURI;
    }

    @Override
    public String getLatestImportURI()
    {
        return latestImportedURI;
    }

    @Override
    public void close()
    {
        if (useConnectorToRetrieveWsdl)
        {
            httpRetrieverStrategy.stopHttpClient();
        }

        closeStreams();
    }


    private void closeStreams()
    {
        for (InputStream stream : streams)
        {
            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                logger.warn("Error closing stream during WSDL retrieval");
            }
        }
    }

    private boolean verifyIsHttpRequester(String url)
    {
        return url.startsWith(HttpConnector.HTTP_URL_PROTOCOL) || url.startsWith(HttpConnector.HTTPS_URL_PROTOCOL);
    }

}
