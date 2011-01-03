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
    public Object transformMessage(MuleMessage message, String encoding) throws TransformerException
    {
        try
        {
            String v = message.getPayloadAsString();
            Map<String, Object> values = new HashMap<String,Object>();
            
            final StringTokenizer tokenizer = new StringTokenizer(v, "&");
            String token;
            while (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken();
                int idx = token.indexOf('=');
                if (idx < 0) {
                    add(values, URLDecoder.decode(token, encoding), null);
                } else if (idx > 0) {
                    add(values, URLDecoder.decode(token.substring(0, idx), encoding),
                                URLDecoder.decode(token.substring(idx+1), encoding));
                }
            }
            return values;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

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
            list.add((String)existingValue);
            list.add(value);
            values.put(key, list);
        }
    }


}
