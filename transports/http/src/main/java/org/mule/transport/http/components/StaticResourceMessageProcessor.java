/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.components;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;

/**
 * A MessageProcessor that can be used by HTTP endpoints to serve static files from a directory on the
 * filesystem.  This processor allows the user to specify a resourceBase which refers to the local directory
 * from where files will be served from. Additionally, a default file can be specificed for URLs where no
 * file is set
 */
public class StaticResourceMessageProcessor implements MessageProcessor, Initialisable
{
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private String resourceBase;
    private String defaultFile = "index.html";
    private MimetypesFileTypeMap mimeTypes;

    @Override
    public void initialise() throws InitialisationException
    {
        mimeTypes = new MimetypesFileTypeMap();
        mimeTypes.addMimeTypes("text/javascript js");
        mimeTypes.addMimeTypes("text/css css");
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (StringUtils.isEmpty(resourceBase))
        {
            throw new ConfigurationException(HttpMessages.noResourceBaseDefined());
        }

        String path = event.getMessage().getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
        String contextPath = event.getMessage().getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);

        // Remove the contextPath from the endpoint from the request as this isn't part of the path.
        path = path.substring(contextPath.length());

        File file = new File(resourceBase + path);
        MuleEvent resultEvent = event;

        if (file.isDirectory() && path.endsWith("/"))
        {
            file = new File(resourceBase + path +  defaultFile);
        }
        else if (file.isDirectory())
        {
            // Return a 302 with the new location
            resultEvent = new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), event.getMuleContext()), event);
            resultEvent.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(HttpConstants.SC_MOVED_TEMPORARILY));
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, 0);
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_LOCATION,
                    event.getMessage().getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY) + "/");
            return resultEvent;
        }

        InputStream in = null;
        try
        {
            in = new FileInputStream(file);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyLarge(in, baos);

            byte[] buffer = baos.toByteArray();

            String mimetype = mimeTypes.getContentType(file);
            if (mimetype == null)
            {
                mimetype = DEFAULT_MIME_TYPE;
            }

            resultEvent = new DefaultMuleEvent(new DefaultMuleMessage(buffer, event.getMuleContext()), event);
            resultEvent.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(HttpConstants.SC_OK));
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, mimetype);
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, buffer.length);
        }
        catch (IOException e)
        {
            throw new ResourceNotFoundException(HttpMessages.fileNotFound(resourceBase + path),event);
        }

        return resultEvent;
    }

    public String getResourceBase()
    {
        return resourceBase;
    }

    public void setResourceBase(String resourceBase)
    {
        this.resourceBase = resourceBase;
    }

    public String getDefaultFile()
    {
        return defaultFile;
    }

    public void setDefaultFile(String defaultFile)
    {
        this.defaultFile = defaultFile;
    }
}
