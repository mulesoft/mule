/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * <code>SimpleHTTPClient</code> is a simple class that handles GET and POST HTTP
 * requests.
 */
public class SimpleHTTPClient
{
    private final static String URL_KEY = "url";
    private final static String METHOD_KEY = "method";

    /**
	 * 
	 */
    public SimpleHTTPClient()
    {
    }

    /**
     * Performs a HTTP GET or HTTP POST request and returns the contents.
     * 
     * @param data <code>Map</code> instance containing the URL (should start with
     *            http://) and the method (GET or POST)
     * @return The contents
     */
    public HTTPResponse httpClient(Object data)
    {
        if (data instanceof Map<?, ?>)
        {
            Map<?, ?> params = (Map<?, ?>) data;

            String url = String.valueOf(params.get(URL_KEY));
            String method = params.containsKey(METHOD_KEY) ? String.valueOf(params.get(METHOD_KEY)) : "GET";

            if (method == null || method.equalsIgnoreCase("GET"))
            {
                return buildResponse(doGet(url));
            }
            else if (method.equalsIgnoreCase("POST"))
            {
                return buildResponse(doPost(url));
            }
            else
            {
                // Invalid method!
                return buildResponse("Error: Invalid method " + method.toUpperCase());
            }
        }
        else
        {
            // No input parameters
            return buildResponse("Error: Missing URL & method input parameters");
        }
    }

    /**
     * @param response
     * @return
     */
    private HTTPResponse buildResponse(String response)
    {
        return new HTTPResponse(response);
    }

    /**
     * @param strUrl
     * @return
     */
    private String doGet(String strUrl)
    {
        BufferedReader rd = null;
        try
        {
            // Send data
            URL url = new URL(strUrl);
            URLConnection conn = url.openConnection();
            conn.setDefaultUseCaches(false);

            // Get the response
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer contents = new StringBuffer();
            while ((line = rd.readLine()) != null)
            {
                contents.append(line);
                contents.append('\n');
            }

            return contents.toString().trim();
        }
        catch (Exception ex)
        {
            return "Error: " + ex.getMessage();
        }
        finally
        {
            close(rd);
        }

    }

    /**
     * @param strUrl
     * @return
     */
    private String doPost(String strUrl)
    {
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try
        {
            // Construct data
            String data = "";

            // Send data
            URL url = new URL(strUrl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDefaultUseCaches(false);

            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();

            // Get the response
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer contents = new StringBuffer();
            while ((line = rd.readLine()) != null)
            {
                contents.append(line);
                contents.append('\n');
            }

            return contents.toString().trim();
        }
        catch (Exception ex)
        {
            return "Error: " + ex.getMessage();
        }
        finally
        {
            close(wr);
            close(rd);
        }
    }

    /**
     * Silently closes a <code>OutputStreamWriter</code>
     * 
     * @param wr
     */
    private void close(OutputStreamWriter wr)
    {
        try
        {
            wr.close();
        }
        catch (Exception e)
        {
            // Do nothing
        }
    }

    /**
     * Silently closes a <code>BufferedReader</code>
     * 
     * @param rd
     */
    private void close(BufferedReader rd)
    {
        try
        {
            rd.close();
        }
        catch (Exception e)
        {
            // Do nothing
        }
    }
}
