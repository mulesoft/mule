/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Converts HTML forms POSTs into a Map of parameters. Each key can have multiple
 * values, in which case the value will be a List&lt;String&gt;. Otherwise, it will
 * be a String.
 */
public class FormTransformer extends AbstractMessageTransformer
{

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            String v = message.getPayloadAsString();
            Map<String, Object> values = new HashMap<String, Object>();

            for (StringTokenizer st = new StringTokenizer(v, "&"); st.hasMoreTokens();)
            {
                String token = st.nextToken();
                int idx = token.indexOf('=');
                if (idx < 0)
                {
                    add(values, URLDecoder.decode(token, outputEncoding), null);
                }
                else if (idx > 0)
                {
                    add(values, URLDecoder.decode(token.substring(0, idx), outputEncoding),
                        URLDecoder.decode(token.substring(idx + 1), outputEncoding));
                }
            }
            return values;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void add(Map<String, Object> values, String key, String value)
    {
        Object existingValue = values.get(key);
        if (existingValue == null)
        {
            values.put(key, value);
        }
        else if (existingValue instanceof List)
        {
            List<String> list = (List<String>) existingValue;
            list.add(value);
        }
        else if (existingValue instanceof String)
        {
            List<String> list = new ArrayList<String>();
            list.add((String) existingValue);
            list.add(value);
            values.put(key, list);
        }
    }
}
