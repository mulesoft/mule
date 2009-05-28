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

import org.mule.api.MessagingException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultMuleMessageDTO;
import org.mule.module.json.filters.IsJsonFilter;
import org.mule.module.json.transformers.JsonToObject;
import org.mule.transport.AbstractMessageAdapter;

import java.util.Map;

/**
 * A message adapter that will accept JSON endcode {@link org.mule.message.DefaultMuleMessageDTO} objects, or a map of objects or just raw
 * payload object.
 * <p/>
 * If the payload is a Map, this adapter will recognise the following keys:
 * <ul>
 * <li>payload - the object to use a the payload, this can be a JSON encoded string. If JSON is used a {@link java.util.Map} will be created. {@link AjaxMessageAdapter.PAYLOAD_PARAM}</li>
 * <li>replyTo - the return ajax channel for this message. {@link AjaxMessageAdapter.REPLYTO_PARAM}</li>
 * </ul>
 * Any additional properties will be set on the message in the {@link org.mule.api.transport.PropertyScope} INVOCATION scope.
 */
public class AjaxMessageAdapter extends AbstractMessageAdapter
{
    public static final String PAYLOAD_PARAM = "payload";
    public static final String REPLYTO_PARAM = "replyTo";

    protected transient JsonToObject transformer;
    protected transient IsJsonFilter filter = new IsJsonFilter();

    protected Object payload;

    public AjaxMessageAdapter(Object message) throws MessagingException
    {
        super();
        init(message);
    }

    public AjaxMessageAdapter(Object message, MessageAdapter template) throws MessagingException
    {
        super(template);
        init(message);
    }

    protected void init(Object message) throws MessagingException
    {
        if (message instanceof Map)
        {
            Map map = (Map) message;
            Object p = map.remove(PAYLOAD_PARAM);
            if (p != null)
            {
                if (filter.accept(p))
                {
                    transformer = new JsonToObject();
                    transformer.setReturnClass(Map.class);
                    try
                    {
                        this.payload = transformer.transform(p);
                    }
                    catch (TransformerException e)
                    {
                        throw new MessagingException(CoreMessages.transformFailed(message.getClass().getName(), Map.class.getName()), message, e);
                    }
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
            transformer = new JsonToObject();
            transformer.setReturnClass(DefaultMuleMessageDTO.class);
            DefaultMuleMessageDTO dto = null;
            try
            {
                dto = (DefaultMuleMessageDTO) transformer.transform(message);
            }
            catch (TransformerException e)
            {
                throw new MessagingException(CoreMessages.transformFailed(message.getClass().getName(), DefaultMuleMessageDTO.class.getName()), message, e);
            }
            payload = dto.getPayload();
            dto.addPropertiesTo(this);
            }
            else
            {
                transformer = new JsonToObject();
                transformer.setReturnClass(Map.class);
                try
                {
                    payload = transformer.transform(message);
                }
                catch (TransformerException e)
                {
                    throw new MessagingException(CoreMessages.transformFailed(message.getClass().getName(), DefaultMuleMessageDTO.class.getName()), message, e);
                }
            }
        }
        else
        {
            payload = message;
        }

    }

    public Object getPayload()
    {
        return payload;
    }
}
