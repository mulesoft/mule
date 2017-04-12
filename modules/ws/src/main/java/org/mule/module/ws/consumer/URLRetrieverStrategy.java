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

import javax.wsdl.WSDLException;


/**
 * A wsdl retriever strategy implementation to get the wsdl directly.
 */
public class URLRetrieverStrategy implements WsdlRetrieverStrategy
{

    @Override
    public InputStream retrieveWsdlResource(String url) throws WSDLException
    {
        InputStream responseStream = null;
        URL location = IOUtils.getResourceAsUrl(url, getClass());

        if (location == null)
        {
            throw new WSDLException("No resource was found on: %s", url.toString());
        }
        
        try
        {
            URLConnection urlConnection = location.openConnection();
            if (location.getUserInfo() != null)
            {
                urlConnection.setRequestProperty("Authorization", "Basic " + encodeBytes(location.getUserInfo().getBytes()));
            }

            responseStream = urlConnection.getInputStream();

            return responseStream;
        }
        catch (Exception e)
        {
            throw new WSDLException("Exception retrieving WSDL for URL: %s", url.toString(), e);
        }
    }
}
