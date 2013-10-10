/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.launcher;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class SimpleDownloadManager
{
    private final static String URL_KEY = "url";

    /**
	 * 
	 */
    public SimpleDownloadManager()
    {
    }

    /**
     * Performs a HTTP GET or HTTP POST request and returns the contents.
     * 
     * @param data <code>Map</code> instance containing the URL (should start with
     *            http://) and the method (GET or POST)
     * @return The contents
     */
    public Object download(Object data)
    {
        String fileUrl = null;
        Map<String, Object> response = new HashMap<String, Object>();

        if (data instanceof Map<?, ?>)
        {
            Map<?, ?> params = (Map<?, ?>) data;

            fileUrl = String.valueOf(params.get(URL_KEY));
        }
        else if (data instanceof String)
        {
            fileUrl = (String) data;
        }

        if (fileUrl != null)
        {
            try
            {
                URL url = new URL(fileUrl);
                URLConnection conn = url.openConnection();
                response.put("is", conn.getInputStream());
                response.put("filename", getFilenameFromUrl(fileUrl));
            }
            catch (Exception ex)
            {
                response.put("error", "Error downloading " + fileUrl + " -> " + ex.getMessage());
            }
        }
        else
        {
            response.put("error", "Not able to get URL to download");
        }
        return response;
    }

    private String getFilenameFromUrl(String fileUrl)
    {
        if (fileUrl != null)
        {
            int i = fileUrl.lastIndexOf("/");
            return fileUrl.substring(i + 1);
        }
        return null;
    }
}
