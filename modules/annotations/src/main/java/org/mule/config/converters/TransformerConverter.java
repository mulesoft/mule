/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.converters;

import org.mule.api.MuleContext;
import org.mule.api.expression.PropertyConverter;
import org.mule.api.transformer.Transformer;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Converts a String list of Transformer names into a List of {@link org.mule.api.transformer.Transformer} objects.
 */
public class TransformerConverter implements PropertyConverter
{
    public static final String DELIM = ",";

    public Object convert(String property, MuleContext context)
    {
        if (null != property)
        {
            List<Transformer> transformers = new LinkedList<Transformer>();
            StringTokenizer st = new StringTokenizer(property, DELIM);
            while (st.hasMoreTokens())
            {
                String key = st.nextToken().trim();
                Transformer transformer = context.getRegistry().lookupTransformer(key);

                if (transformer == null)
                {
                    throw new IllegalArgumentException(key);
                }
                transformers.add(transformer);
            }
            return transformers;
        }
        else
        {
            return null;
        }

    }

    public Class getType()
    {
        return TransformerList.class;
    }
}
