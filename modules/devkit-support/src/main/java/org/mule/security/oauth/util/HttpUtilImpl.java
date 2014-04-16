/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for {@link org.mule.security.oauth.util.HttpUtil}
 */
public class HttpUtilImpl implements HttpUtil
{

    private static final transient Logger logger = LoggerFactory.getLogger(HttpUtilImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String post(String url, String body)
    {
        HttpURLConnection conn = null;
        try
        {
            conn = ((HttpURLConnection) new URL(url).openConnection());
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(url + " is not a valid url", e);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not open connection to " + url, e);
        }

        try
        {
            conn.setRequestMethod("POST");
        }
        catch (ProtocolException e)
        {
            throw new RuntimeException(
                "Something is wrong with the runtime, POST is not recognized as a verb", e);
        }

        conn.setDoOutput(true);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Sending request to [%s] using the following as content [%s]", url,
                body));
        }

        try
        {
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(body);
            out.close();

            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
            	return IOUtils.toString(conn.getInputStream());
            } else {
                String errorMsg = IOUtils.toString(conn.getErrorStream());
                logger.error(String.format("Received [%d] for body [%s]", responseCode, errorMsg));

                throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + url);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error found while consuming http resource at " + url, e);
        }
    }

}
