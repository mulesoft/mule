/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.spi;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.module.ibeans.spi.support.DataTypeConverter;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;

import java.io.InputStream;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.MimeTypeParseException;

import org.ibeans.api.DataType;
import org.ibeans.api.Response;
import org.ibeans.api.channel.MimeTypes;

/**
 * An implementation of an IBeans {@link org.ibeans.api.Response} that adapts to a {@link org.mule.api.MuleMessage}
 */
public class MuleResponseMessage implements Response
{
    private MuleMessage message;
    private DataType dataType;
    private String status;

    public MuleResponseMessage(MuleMessage message) throws MimeTypeParseException
    {
        this.message = message;
        //TODO should DataType ever be null?
        if(message.getDataType()==null)
        {
            //s this is response
            String mime = message.findPropertyInAnyScope(MuleProperties.CONTENT_TYPE_PROPERTY,null);
            if (mime == null)
            {
                //case insensitive
                mime = message.findPropertyInAnyScope("ContentType", null);
            }
            if(mime==null) mime = MimeTypes.ANY.getBaseType();

            dataType = org.ibeans.impl.support.datatype.DataTypeFactory.create(message.getPayload().getClass(), mime);
        }
        else
        {
            dataType = DataTypeConverter.convertMuleToIBeans(message.getDataType());
        }
        status = message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
    }

    public String getStatusCode()
    {
        //TODO this will be null for non-http
        return status;
    }

    public void setStatusCode(String code)
    {
        this.status = code;
    }

    public String getMimeType()
    {
        return dataType.getMimeType();
    }

    public DataType getDataType()
    {
        return dataType;
    }

    public InputStream getPayloadAsStream()
    {
        if(message.getPayload() instanceof NullPayload)
        {
            return null;
        }
        try
        {
            return message.getPayload(InputStream.class);
        }
        catch (TransformerException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public Object getPayload()
    {
        if(message.getPayload() instanceof NullPayload)
        {
            return null;
        }
        return message.getPayload();
    }

    public Object getHeader(String name)
    {
        return message.getInboundProperty(name);
    }

    public Set<String> getHeaderNames()
    {
        return message.getInboundPropertyNames();
    }

    public DataHandler getAttachment(String name)
    {
        return message.getInboundAttachment(name);
    }

    public Set<String> getAttachmentNames()
    {
        return message.getInboundAttachmentNames();
    }

    public MuleMessage getMessage()
    {
        return message;
    }

    public Throwable getException()
    {
        if(message.getExceptionPayload()!=null)
        {
            return message.getExceptionPayload().getRootException();
        }
        return null;
    }


}

