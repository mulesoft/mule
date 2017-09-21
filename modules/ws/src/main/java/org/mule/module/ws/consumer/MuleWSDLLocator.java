/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static org.apache.xmlbeans.impl.schema.StscImporter.resolveRelativePathInArchives;
import static org.mule.module.ws.consumer.WSDLUtils.getBasePath;
import static org.mule.transport.http.HttpConnector.HTTPS_URL_PROTOCOL;
import static org.mule.transport.http.HttpConnector.HTTP_URL_PROTOCOL;
import static org.apache.commons.io.FilenameUtils.normalize;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.ws.consumer.wsdl.strategy.factory.HttpRequesterWsdlRetrieverStrategyFactory;
import org.mule.module.ws.consumer.wsdl.strategy.factory.URLWSDLRetrieverStrategyFactory;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLLocator;

import org.apache.xmlbeans.impl.common.HttpRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * A custom locator to replicate the context of requester in every step 
 * where wsdl4j has to retrieve a resource (imported XSD, WSDL)
 */
public class MuleWSDLLocator implements WSDLLocator, HttpRetriever
{
    private static final Logger logger = LoggerFactory.getLogger(WSConsumer.class);
    public static final String JAR = "jar";
    public static final String ZIP = "zip";

    private String baseURI;
    private String latestImportedURI;
    private boolean useConnectorToRetrieveWsdl;
    private Collection<InputStream> streams = new ArrayList<InputStream>();
    private TlsContextFactory tlsContextFactory;
    private ProxyConfig proxyConfig;
    private MuleContext muleContext;

    public MuleWSDLLocator(MuleWSDLLocatorConfig config) throws MuleException
    {
        this.baseURI = getAbsoluteURI(config.getBaseURI());
        this.useConnectorToRetrieveWsdl = config.isUseConnectorToRetrieveWsdl();
        this.tlsContextFactory = config.getTlsContextFactory();
        this.muleContext = config.getContext();
        this.proxyConfig = config.getProxyConfig();
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
            if (isHttpAddress(importLocation))
            {
                latestImportedURI = importLocation;
            }
            else
            {
                URL url = IOUtils.getResourceAsUrl(parentLocation, getClass());
                if (mustResolveRelativePaths(url))
                {
                    latestImportedURI = resolveRelativePathInArchives(normalize(getBasePath(url.toString())) + importLocation);
                }
                else
                {
                    latestImportedURI = normalize((getBasePath(url.toString()) + importLocation));
                }

            }

            return getInputSource(latestImportedURI);
        }
        catch (Exception e)
        {
            throw new RuntimeException("There has been an error retrieving the following wsdl resource: " + latestImportedURI, e);
        }
    }

    private boolean mustResolveRelativePaths(URL url)
    {
        return url.getProtocol().equals(JAR) || url.getProtocol().equals(ZIP);
    }

    private InputSource getInputSource(String url) throws WSDLException
    {
        InputStream resultStream;
        try
        {
            resultStream = getStreamFrom(url);
        }
        catch (Exception e)
        {
            throw new WSDLException(WSDLException.OTHER_ERROR, e.getMessage(), e);
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

    private boolean isHttpAddress(String url)
    {
        return url.startsWith(HTTP_URL_PROTOCOL) || url.startsWith(HTTPS_URL_PROTOCOL);
    }

    @Override
    public InputStream getStreamFrom(String url) throws Exception
    {
        boolean isHttpRequester = isHttpAddress(url);

        InputStream resultStream = null;

        if (useConnectorToRetrieveWsdl && isHttpRequester)
        {
            resultStream = new HttpRequesterWsdlRetrieverStrategyFactory(tlsContextFactory, proxyConfig, muleContext)
                                                                                                                     .createWSDLRetrieverStrategy().retrieveWsdlResource(url);
        }
        else
        {
            resultStream = new URLWSDLRetrieverStrategyFactory().createWSDLRetrieverStrategy().retrieveWsdlResource(url);
        }
        
        return resultStream;
    }
    
    private String getAbsoluteURI(String uri)
    {
        if (uri != null)
        {
            URL absoluteURI = IOUtils.getResourceAsUrl(uri, getClass());
            return (absoluteURI == null) ? null : absoluteURI.toString();
        }

        return uri;
    }

}
