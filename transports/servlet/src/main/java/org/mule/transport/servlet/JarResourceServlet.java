/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for loading resources loaded in jar files
 */
public class JarResourceServlet extends HttpServlet
{
    public static final String DEFAULT_PATH_SPEC = "/mule-resource/*";

    public static final String DEFAULT_BASE_PATH = "";

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private String basepath = DEFAULT_BASE_PATH;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String file = getBasepath() + req.getPathInfo();

        if(file.startsWith("/")) file = file.substring(1);
        InputStream in = IOUtils.getResourceAsStream(file, getClass(), false, false);
        if (in == null)
        {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Unable to find file: " + req.getPathInfo());
            return;
        }
        byte[] buffer;
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyLarge(in, baos);

            buffer = baos.toByteArray();

            String mimetype = DEFAULT_MIME_TYPE;
            if (getServletContext() != null)
            {
                String temp = getServletContext().getMimeType(file);
                if (temp != null)
                {
                    mimetype = temp;
                }
            }

            resp.setContentType(mimetype);
            resp.setContentLength(buffer.length);
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + req.getPathInfo() + "\"");
            resp.getOutputStream().write(buffer);
        }
        finally
        {
            in.close();
            resp.getOutputStream().flush();
        }
    }

    public String getBasepath()
    {
        return basepath;
    }

    public void setBasepath(String basepath)
    {
        this.basepath = basepath;
    }
}
