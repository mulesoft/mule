/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.converters;

import org.mule.api.MuleContext;
import org.mule.api.expression.PropertyConverter;
import org.mule.util.StringUtils;

import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Converts a comma-separated list of key/value pairs, e.g.,
 * <code>"apple=green, banana=yellow"</code> into a {@link java.util.Properties} map.
 * Property placeholders can be used in these values:
 * <code>"apple=${apple.color}, banana=yellow"</code> 
 */
public class PropertiesConverter implements PropertyConverter
{
    public static final String DELIM = ",";

    public Object convert(String properties, MuleContext context)
    {
        if (StringUtils.isNotBlank(properties))
        {
            Properties props = new Properties();
            
            StringTokenizer st = new StringTokenizer(properties, DELIM);
            while (st.hasMoreTokens())
            {
                String key = st.nextToken().trim();
                int i = key.indexOf("=");
                if(i < 1) {
                    throw new IllegalArgumentException("Property string is malformed: " + properties);
                }
                String value = key.substring(i+1);
                key = key.substring(0, i);
                props.setProperty(key, value);
            }
            return props;
        }
       return null;
    }

    public Class getType()
    {
        return Properties.class;
    }
}
