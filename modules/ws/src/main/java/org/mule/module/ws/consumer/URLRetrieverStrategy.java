/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.wsdl.WSDLException;

import org.mule.util.Base64;

public class URLRetrieverStrategy implements WsdlRetrieverStrategy
{

    @Override
    public InputStream retrieveWsdl(URL url) throws WSDLException
    {
        try
        {
            URLConnection urlConnection = url.openConnection();

            if (url.getUserInfo() != null)
            {
                urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(url.getUserInfo().getBytes()));
            }

            return urlConnection.getInputStream();
        }
        catch (IOException e)
        {
            throw new WSDLException("Could not retrieve wsdl %s", url.toString(), e);
        }
    }

}
