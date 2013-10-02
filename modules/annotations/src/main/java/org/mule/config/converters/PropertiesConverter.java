/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
