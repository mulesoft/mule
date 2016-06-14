/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.message.ds.ByteArrayDataSource;
import org.mule.runtime.core.message.ds.InputStreamDataSource;
import org.mule.runtime.core.message.ds.StringDataSource;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

/**
 * Base component to create HTTP messages.
 *
 * @since 4.0
 */
public class HttpMessageBuilder
{
    /**
     * HTTP headers the message should include.
     */
    @Parameter
    @Optional
    protected Map<String, String> headers = new HashMap<>();

    /**
     * HTTP parts the message should include.
     */
    @Parameter
    @Optional
    protected List<HttpPart> parts = new LinkedList<>();

    public Map<String, String> getHeaders()
    {
        return headers;
    }

    public Map<String, DataHandler> getParts()
    {
        return getResolvedParts(parts);
    }

    protected Map<String, DataHandler> getResolvedParts(List<HttpPart> parts)
    {
        Map<String, DataHandler> resolvedAttachments = new HashMap<>();

        parts.forEach(attachment -> {
            String filename = attachment.getFilename();
            String name = filename != null ? filename : attachment.getId();
            DataHandler dataHandler;
            try
            {
                dataHandler = toDataHandler(name, attachment.getData(), attachment.getContentType());
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(createStaticMessage("Could not create part %s", attachment.getId()), e);
            }
            resolvedAttachments.put(attachment.getId(), dataHandler);
        });

        return resolvedAttachments;
    }

    //This will be in IOUtils
    public static DataHandler toDataHandler(String name, Object object, String contentType) throws Exception
    {
        DataHandler dh;
        if (object instanceof File)
        {
            if (contentType != null)
            {
                dh = new DataHandler(new FileInputStream((File) object), contentType);
            }
            else
            {
                dh = new DataHandler(new FileDataSource((File) object));
            }
        }
        else if (object instanceof URL)
        {
            if (contentType != null)
            {
                dh = new DataHandler(((URL) object).openStream(), contentType);
            }
            else
            {
                dh = new DataHandler((URL) object);
            }
        }
        else if (object instanceof String)
        {
            if (contentType != null)
            {
                dh = new DataHandler(new StringDataSource((String) object, name, contentType));
            }
            else
            {
                dh = new DataHandler(new StringDataSource((String) object, name));
            }
        }
        else if (object instanceof byte[] && contentType != null)
        {
            dh = new DataHandler(new ByteArrayDataSource((byte[]) object, contentType, name));
        }
        else if (object instanceof InputStream && contentType != null)
        {
            dh = new DataHandler(new InputStreamDataSource((InputStream) object, contentType, name));
        }
        else
        {
            dh = new DataHandler(object, contentType);
        }
        return dh;
    }
}
