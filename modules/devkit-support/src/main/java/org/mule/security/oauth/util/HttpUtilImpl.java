/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
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

        OutputStreamWriter out = null;
        InputStream errorStream = null;
        try
        {
            out = new OutputStreamWriter(conn.getOutputStream());
            out.write(body);
            out.flush();

            int responseCode = conn.getResponseCode();

            if (wasSuccessful(responseCode))
            {
            	return IOUtils.toString(conn.getInputStream());
            } 
            else 
            {
                errorStream = conn.getErrorStream();
                String response = IOUtils.toString(errorStream);
                String errorMsg = String.format("Received status code [%d] while trying to get OAuth2 verification code. Response body was [%s]", responseCode, response);
                logger.error(errorMsg);  
                throw new IOException(errorMsg);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error found while consuming http resource at " + url, e);
        }
        finally
        {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(errorStream);
        }
    }

    private boolean wasSuccessful(int responseCode) throws IOException
    {
        return responseCode >= 200 && responseCode <= 203;
    }

}
