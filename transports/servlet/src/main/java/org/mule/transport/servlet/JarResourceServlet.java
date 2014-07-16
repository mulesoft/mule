/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
 * A servlet for loading resources loaded in jar files. This allows javascript, html
 * and images to be bundled into a jar This servlet also supports property
 * placeholders for html, xml and json files. This allows for server configuration to
 * be injected into static files.
 */
public class JarResourceServlet extends HttpServlet
{
    public static final String DEFAULT_PATH_SPEC = "/mule-resource/*";

    public static final String DEFAULT_BASE_PATH = "";

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private String basepath = DEFAULT_BASE_PATH;

    private String[] templateExtensions = new String[] { "htm", "html", "xml", "json" };

    private TemplateParser templateParser = TemplateParser.createAntStyleParser();

    private MuleContext muleContext;

    private Map<?, ?> properties;

    @Override
    public void init() throws ServletException
    {
        muleContext = (MuleContext) getServletContext().getAttribute(MuleProperties.MULE_CONTEXT_PROPERTY);

        // We need MuleContext for doing templating
        if (muleContext == null)
        {
            throw new ServletException("Property " + MuleProperties.MULE_CONTEXT_PROPERTY
                                       + " not set on ServletContext");
        }

        properties = new RegistryMap(muleContext.getRegistry());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String file = getFile(request);

        InputStream in = IOUtils.getResourceAsStream(file, getClass(), false, false);
        if (in == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Unable to find file: " + request.getPathInfo());
            return;
        }

        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyLarge(in, baos);
            byte[] buffer = baos.toByteArray();

            String mimetype = determineMimeType(file);
            buffer = expandTemplates(buffer, mimetype);

            response.setContentType(mimetype);
            response.setContentLength(buffer.length);
            if (mimetype.equals(DEFAULT_MIME_TYPE))
            {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + request.getPathInfo() + "\"");
            }
            response.getOutputStream().write(buffer);
        }
        finally
        {
            in.close();
            response.getOutputStream().flush();
        }
    }

    protected String getFile(HttpServletRequest request)
    {
        String file = getBasepath() + request.getPathInfo();
        if (file.startsWith("/"))
        {
            file = file.substring(1);
        }
        return file;
    }

    protected String determineMimeType(String file)
    {
        String mimetype = DEFAULT_MIME_TYPE;
        if (getServletContext() != null)
        {
            String temp = getServletContext().getMimeType(file);
            if (temp != null)
            {
                mimetype = temp;
            }
        }
        return mimetype;
    }

    protected byte[] expandTemplates(byte[] buffer, String mimetype)
    {
        // We could wrap this parsing in a stream to make it more efficient
        for (String extension : templateExtensions)
        {
            if (mimetype.endsWith(extension))
            {
                return templateParser.parse(properties, new String(buffer)).getBytes();
            }
        }
        return buffer;
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
