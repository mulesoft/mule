/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd;

import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for loading resources loaded in jar files
 */
public class MuleJarResourcesServlet extends HttpServlet
{
    public static final String BASE_PATH = "META-INF";

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String file = BASE_PATH + req.getPathInfo();

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
}
