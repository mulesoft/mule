/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.DefaultMuleMessage;
import org.mule.api.DefaultMuleException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.api.transport.PropertyScope;
import org.mule.module.json.JsonData;
import org.mule.module.json.filters.IsJsonFilter;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.ajax.embedded.AjaxConnector;

import java.io.IOException;
import java.util.Map;

import org.cometd.Bayeux;

/**
 * A {@link MuleMessageFactory} implementation for JSON messages. The payload can either be a 
 * {@link Map} or a JSON encoded String.
 * <p/>
 * If the payload is a {@link Map}, this message factory will recognise the following keys:
 * <ul>
 * <li>data - the object to use a the payload, this can be a JSON encoded string. See {@link Bayeux#DATA_FIELD}</li>
 * <li>replyTo - the return Ajax channel for this message. {@link AjaxConnector#REPLYTO_PARAM}</li>
 * </ul>
 */
public class AjaxMuleMessageFactory extends AbstractMuleMessageFactory
{
    protected transient IsJsonFilter filter = new IsJsonFilter();

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[] { Object.class };
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        if (transportMessage instanceof Map<?, ?>)
        {
            return extractPayloadFromMap((Map<?, ?>) transportMessage);
        }
        else if (filter.accept(transportMessage))
        {
            return extractJsonPayload(transportMessage);
        }
        else
        {
            return transportMessage;
        }
    }
    
    @Override
    protected void addProperties(DefaultMuleMessage muleMessage, Object transportMessage) throws Exception
    {
        if (transportMessage instanceof Map<?, ?>)
        {
            addPropertiesFromMap(muleMessage, (Map<?, ?>) transportMessage);
        }
        else if (filter.accept(transportMessage))
        {
            addPropertiesToFromJsonData(muleMessage, transportMessage);
        }
    }

    private Object extractPayloadFromMap(Map<?, ?> map)
    {
        Object data = map.remove(Bayeux.DATA_FIELD);
        if (data == null)
        {
            throw new IllegalArgumentException(Bayeux.DATA_FIELD + " parameter not set in payload map"); 
        }
        return data;
    }

    private Object extractJsonPayload(Object transportMessage) throws DefaultMuleException
    {
        String transportMessageString = transportMessage.toString();
        if (transportMessageString.indexOf(Bayeux.DATA_FIELD) > -1)
        {
            try
            {
                JsonData data = new JsonData(transportMessageString);
                return data.get(Bayeux.DATA_FIELD).toString();
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
        else
        {
            return transportMessage;
        }
    }

    @SuppressWarnings("unchecked")
    private void addPropertiesFromMap(DefaultMuleMessage muleMessage, Map<?, ?> map)
    {
        Object replyTo = map.remove(AjaxConnector.REPLYTO_PARAM);
        muleMessage.setReplyTo(replyTo);

        // remove the part of the map we process as payload
        map.remove(Bayeux.DATA_FIELD);
        
        // the remainder of the map is used as message properties
        Map<String, Object> messageProperties = (Map<String, Object>) map;
        muleMessage.addProperties(messageProperties, PropertyScope.INVOCATION);
    }
    
    private void addPropertiesToFromJsonData(DefaultMuleMessage muleMessage, Object transportMessage) throws IOException
    {
        JsonData data = new JsonData(transportMessage.toString());
        if (data.hasNode(AjaxConnector.REPLYTO_PARAM))
        {
            muleMessage.setReplyTo(data.getAsString(AjaxConnector.REPLYTO_PARAM));
        }
    }
}
