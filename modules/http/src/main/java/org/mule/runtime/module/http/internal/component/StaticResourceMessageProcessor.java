/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.component;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_LISTENER_PATH;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.LOCATION;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.api.message.NullPayload;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

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
    public static final String ANY_PATH = "/*";
    public static final String ROOT_PATH = "/";

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
            throw new ConfigurationException(createStaticMessage("No ResourceBase Defined as part of the static resource message processor."));
        }

        String path = event.getMessage().getInboundProperty(HTTP_REQUEST_PATH_PROPERTY);
        String contextPath = event.getMessage().getInboundProperty(HTTP_LISTENER_PATH);

        //Get rid of ending wildcards.
        if (contextPath.equals(ANY_PATH))
        {
            contextPath = ROOT_PATH;
        }
        if (contextPath.endsWith(ANY_PATH))
        {
            contextPath = StringUtils.removeEnd(contextPath, ANY_PATH);
        }

        if (!ROOT_PATH.equals(contextPath))
        {
            // Remove the contextPath from the endpoint from the request as this isn't part of the path.
            path = path.substring(contextPath.length());
        }

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
            resultEvent.getMessage().setOutboundProperty(HTTP_STATUS_PROPERTY, String.valueOf(HttpConstants.HttpStatus.MOVED_TEMPORARILY.getStatusCode()));
            resultEvent.getMessage().setOutboundProperty(CONTENT_LENGTH, 0);
            resultEvent.getMessage().setOutboundProperty(LOCATION,
                    event.getMessage().getInboundProperty(HTTP_REQUEST_PATH_PROPERTY) + "/");
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
            resultEvent.getMessage().setOutboundProperty(HTTP_STATUS_PROPERTY, String.valueOf(OK.getStatusCode()));
            resultEvent.getMessage().setOutboundProperty(CONTENT_TYPE, mimetype);
            resultEvent.getMessage().setOutboundProperty(CONTENT_LENGTH, buffer.length);
        }
        catch (IOException e)
        {
            throw new ResourceNotFoundException(createStaticMessage(format("The file: %s was not found.", resourceBase + path)), event, this);
        }
        finally
        {
            IOUtils.closeQuietly(in);
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