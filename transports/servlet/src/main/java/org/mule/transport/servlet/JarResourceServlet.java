/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.registry.RegistryMap;
import org.mule.util.IOUtils;
import org.mule.util.TemplateParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for loading resources loaded in jar files. This allows javascript, htm and images to be bundled into a jar
 * This servlet also supports property placeholders for html, xml and json files.  This allows for server configuration
 * to be injected into static files.
 */
public class JarResourceServlet extends HttpServlet
{
    public static final String DEFAULT_PATH_SPEC = "/mule-resource/*";

    public static final String DEFAULT_BASE_PATH = "";

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private String basepath = DEFAULT_BASE_PATH;

    private String[] templateExtensions = new String[]{"htm", "html", "xml", "json"};

    private TemplateParser parser = TemplateParser.createAntStyleParser();

    private MuleContext muleContext;

    private Map props;

    @Override
    public void init() throws ServletException
    {
        muleContext = (MuleContext) getServletContext().getAttribute(MuleProperties.MULE_CONTEXT_PROPERTY);
        //We need MuleContext for doing templating
        if (muleContext == null)
        {
            throw new ServletException("Property " + MuleProperties.MULE_CONTEXT_PROPERTY + " not set on ServletContext");
        }

        props = new RegistryMap(muleContext.getRegistry());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String file = getBasepath() + req.getPathInfo();

        if (file.startsWith("/"))
        {
            file = file.substring(1);
        }

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

            //We could wrap this parsing in a stream to make it more efficient
            for (String extension : templateExtensions)
            {
                if(mimetype.endsWith(extension))
                {
                    buffer = parser.parse(props, new String(buffer)).getBytes();
                    break;
                }
            }
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
