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
import org.mule.message.DefaultMuleMessageDTO;
import org.mule.module.json.JsonData;
import org.mule.module.json.filters.IsJsonFilter;
import org.mule.transport.AbstractMessageAdapter;

import java.io.IOException;
import java.util.Map;

import org.cometd.Bayeux;

/**
 * A message adapter that will accept JSON endcode {@link DefaultMuleMessageDTO} objects,
 * or a map of objects or just raw payload object.
 * <p/>
 * If the payload is a Map, this adapter will recognise the following keys:
 * <ul>
 * <li>data - the object to use a the payload, this can be a JSON encoded string. {@link Bayeux#DATA_FIELD}</li>
 * <li>replyTo - the return ajax channel for this message. {@link AjaxMessageAdapter#REPLYTO_PARAM}</li>
 * </ul>
 */
public class AjaxMessageAdapter extends AbstractMessageAdapter
{
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
        //Cometd will pass us a map if sent fromth
        if (message instanceof Map)
        {
            Map map = (Map) message;
            Object p = map.remove(Bayeux.DATA_FIELD);
            if (p != null)
            {
                payload = p;
            }
            else
            {
                throw new IllegalArgumentException("data parameter not set");
            }

            setReplyTo(map.remove(REPLYTO_PARAM));

            if (map.size() > 0)
            {
                addProperties(map, PropertyScope.INVOCATION);
            }
        }
        else if (filter.accept(message))
        {
            if (message.toString().indexOf(Bayeux.DATA_FIELD) > -1)
            {
                try
                {
                    JsonData jd = new JsonData(message.toString());
                    payload = jd.get(Bayeux.DATA_FIELD).toString();
                    setReplyTo(jd.get(REPLYTO_PARAM));
                }
                catch (IOException e)
                {
                    throw new DefaultMuleException(e);
                }
            }
            else
            {
                this.payload = message;
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
