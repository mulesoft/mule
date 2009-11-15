/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ajax;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultMuleMessageDTO;
import org.mule.module.json.filters.IsJsonFilter;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * A message adapter that will accept JSON endcode {@link DefaultMuleMessageDTO} objects, 
 * or a map of objects or just raw payload object.
 * <p/>
 * If the payload is a Map, this adapter will recognise the following keys:
 * <ul>
 * <li>payload - the object to use a the payload, this can be a JSON encoded string. 
 * If JSON is used a {@link java.util.Map} will be created. {@link AjaxMessageAdapter#PAYLOAD_PARAM}</li>
 * <li>replyTo - the return ajax channel for this message. {@link AjaxMessageAdapter#REPLYTO_PARAM}</li>
 * </ul>
 * Any additional properties will be set on the message in the {@link PropertyScope#INVOCATION} scope.
 */
public class AjaxMessageAdapter extends AbstractMessageAdapter
{
    public static final String PAYLOAD_PARAM = "payload";
    public static final String REPLYTO_PARAM = "replyTo";

    protected transient IsJsonFilter filter = new IsJsonFilter();

    protected Object payload;

    public AjaxMessageAdapter(Object message) throws MuleException
    {
        super();
        init(message);
    }

    public AjaxMessageAdapter(Object message, MessageAdapter template) throws MuleException
    {
        super(template);
        init(message);
    }

    protected void init(Object message) throws MuleException
    {
        if (message instanceof Map)
        {
            Map map = (Map) message;
            Object p = map.remove(PAYLOAD_PARAM);
            if (p != null)
            {
                if (filter.accept(p))
                {
                    this.payload = readJsonAs(p, HashMap.class);
                }
                else
                {
                    payload = p;
                }
            }
            else
            {
                throw new IllegalArgumentException("payload parameter not set");
            }

            setReplyTo(map.remove(REPLYTO_PARAM));

            if (map.size() > 0)
            {
                addProperties(map, PropertyScope.INVOCATION);
            }
        }
        else if (filter.accept(message))
        {
            if( message.toString().indexOf("payload") > -1)
            {
            DefaultMuleMessageDTO dto = readJsonAs(message, DefaultMuleMessageDTO.class);
            payload = dto.getPayload();
            dto.addPropertiesTo(this);
            }
            else
            {
               this.payload = readJsonAs(message, HashMap.class);
            }
        }
        else
        {
            payload = message;
        }

    }

    protected <T> T readJsonAs(Object src, Class<T> type) throws DefaultMuleException
    {
        InputStream in = null;
         try
        {
        ObjectMapper mapper = new ObjectMapper();
        in = getObjectAsStream(src);

            return mapper.readValue(in, type);
        }
         catch(IOException e)
         {
             throw new DefaultMuleException(CoreMessages.failedToReadPayload(), e);
         }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    protected InputStream getObjectAsStream(Object src) throws IOException
    {
        if (src instanceof InputStream)
            {
                return (InputStream) src;
            }
            else if (src instanceof File)
            {
                return new FileInputStream((File) src);
            }
            else if (src instanceof URL)
            {
                return ((URL) src).openStream();
            }
            else if (src instanceof byte[])
            {
                return new ByteArrayInputStream((byte[]) src);
            }
        else if (src instanceof String)
            {
                return new ByteArrayInputStream(((String) src).getBytes());
            }
        else
        {
            throw new IllegalArgumentException("OBject type not supported for JSON transform: " + src);
        }
    }

    public Object getPayload()
    {
        return payload;
    }
}
