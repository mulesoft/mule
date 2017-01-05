/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static org.mule.util.Base64.encodeBytes;

import org.mule.util.IOUtils;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;


/**
 * A wsdl retriever strategy implementation to get the wsdl directly.
 */
public class URLRetrieverStrategy extends AbstractInputStreamStrategy
{

    private URL url;

    public URLRetrieverStrategy(String url)
    {
        this.url = IOUtils.getResourceAsUrl(url, getClass());
    }

    @Override
    public Definition retrieveWsdlFrom() throws WSDLException
    {
        try
        {
            InputStream responseStream = null;
            Definition wsdlDefinition = null;

            URLConnection urlConnection = url.openConnection();

            if (url.getUserInfo() != null)
            {
                urlConnection.setRequestProperty("Authorization", "Basic " + encodeBytes(url.getUserInfo().getBytes()));
            }

            responseStream = urlConnection.getInputStream();

            wsdlDefinition = getWsdlDefinition(url.toString(), responseStream);
            responseStream.close();
            return wsdlDefinition;
        }
        catch (Exception e)
        {
            throw new WSDLException("Exception retrieving WSDL for URL: %s", url.toString(), e);
        }
    }

}
