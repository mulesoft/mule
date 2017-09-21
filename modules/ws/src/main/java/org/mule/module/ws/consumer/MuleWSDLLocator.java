/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static java.lang.ClassLoader.getSystemResource;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.xmlbeans.impl.schema.StscImporter.resolveRelativePathInArchives;
import static org.mule.module.ws.consumer.WSDLUtils.getBasePath;
import static org.mule.transport.http.HttpConnector.HTTPS_URL_PROTOCOL;
import static org.mule.transport.http.HttpConnector.HTTP_URL_PROTOCOL;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLLocator;

import org.apache.xmlbeans.impl.common.HttpRetriever;
import org.mule.api.MuleContext;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.ws.consumer.wsdl.strategy.factory.HttpRequesterWsdlRetrieverStrategyFactory;
import org.mule.module.ws.consumer.wsdl.strategy.factory.URLWSDLRetrieverStrategyFactory;
import org.mule.transport.ssl.api.TlsContextFactory;
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

    public MuleWSDLLocator(MuleWSDLLocatorConfig config) throws URISyntaxException, IOException
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
                URI uri = new URI(parentLocation);
                if (mustResolveRelativePaths(uri))
                {
                    latestImportedURI = resolveRelativePathInArchives(normalize(getBasePath(uri.toString())) + importLocation);
                }
                else
                {
                    latestImportedURI = normalize((getBasePath(uri.toString()) + importLocation));
                }

            }

            return getInputSource(latestImportedURI);
        }
        catch (Exception e)
        {
            throw new RuntimeException("There has been an error retrieving the following wsdl resource: " + latestImportedURI, e);
        }
    }

    private boolean mustResolveRelativePaths(URI uri)
    {
        return uri.getScheme().equals(JAR) || uri.getScheme().equals(ZIP);
    }

    private InputSource getInputSource(String uri) throws WSDLException
    {
        InputStream resultStream;
        try
        {
            resultStream = getStreamFrom(uri);
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

    private boolean isHttpAddress(String uri)
    {
        return uri.startsWith(HTTP_URL_PROTOCOL) || uri.startsWith(HTTPS_URL_PROTOCOL);
    }

    @Override
    public InputStream getStreamFrom(String uri) throws Exception
    {
        boolean isHttpRequester = isHttpAddress(uri);

        InputStream resultStream = null;

        if (useConnectorToRetrieveWsdl && isHttpRequester)
        {
            resultStream = new HttpRequesterWsdlRetrieverStrategyFactory(tlsContextFactory, proxyConfig, muleContext)
                                                                                                                     .createWSDLRetrieverStrategy().retrieveWsdlResource(uri);
        }
        else
        {
            resultStream = new URLWSDLRetrieverStrategyFactory().createWSDLRetrieverStrategy().retrieveWsdlResource(uri);
        }
        
        return resultStream;
    }
    
    private String getAbsoluteURI(String uri) throws URISyntaxException, IOException
    {
        if (uri != null)
        {
            URI absoluteURI = new URI(uri);
            if (absoluteURI.getScheme() == null)
            {
                return getSystemResource(uri).toString();
            }
            return (absoluteURI == null) ? null : absoluteURI.toString();
        }

        return uri;
    }

}
