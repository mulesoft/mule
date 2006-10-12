/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

/**
 * <code>PropertiesHelper</code> is a utility class for manipulating and filtering
 * property Maps.
 */
// @ThreadSafe
public class PropertiesUtils
{
    // @GuardedBy(itself)
    private static final List maskedProperties = new CopyOnWriteArrayList();

    static
    {
        // When printing property lists mask password fields
        // Users can register their own fields to mask
        registerMaskedPropertyName("password");
    }

    /**
     * Register a property name for masking. This will prevent certain values from
     * leaking e.g. into debugging output or logfiles.
     * 
     * @param name the key of the property to be masked.
     * @throws IllegalArgumentException is name is null or empty.
     */
    public static void registerMaskedPropertyName(String name)
    {
        if (StringUtils.isNotEmpty(name))
        {
            maskedProperties.add(name);
        }
        else
        {
            throw new IllegalArgumentException("Cannot mask empty property name.");
        }
    }

    /**
     * Returns the String representation of the property value or a masked String if
     * the property key has been registered previously via
     * {@link #registerMaskedPropertyName(String)}.
     * 
     * @param property a key/value pair
     * @return String of the property value or a "masked" String that hides the
     *         contents.
     */
    public static String maskedPropertyValue(Map.Entry property)
    {
        if (maskedProperties.contains(property.getKey()))
        {
            return ("*****");
        }
        else
        {
            return property.getValue().toString();
        }
    }

    /**
     * Read in the properties from a properties file. The file may be on the file
     * system or the classpath.
     * 
     * @param fileName - The name of the properties file
     * @param callingClass - The Class which is calling this method. This is used to
     *            determine the classpath.
     * @return a java.util.Properties object containing the properties.
     */
    public static synchronized Properties loadProperties(String fileName, final Class callingClass)
        throws IOException
    {
        InputStream is = IOUtils.getResourceAsStream(fileName, callingClass,
        /* tryAsFile */true, /* tryAsUrl */false);
        if (is == null)
        {
            Message error = new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE, fileName);
            throw new IOException(error.toString());
        }

        try
        {
            Properties props = new Properties();
            props.load(is);
            return props;
        }
        finally
        {
            is.close();
        }
    }

    public static String removeXmlNamespacePrefix(String eleName)
    {
        int i = eleName.indexOf(':');
        return eleName.substring(i + 1, eleName.length());
    }

    public static String removeNamespacePrefix(String eleName)
    {
        int i = eleName.lastIndexOf('.');
        return eleName.substring(i + 1, eleName.length());
    }

    public static Map removeNamespaces(Map properties)
    {
        HashMap props = new HashMap(properties.size());
        Map.Entry entry;
        for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();)
        {
            entry = (Map.Entry)iter.next();
            props.put(removeNamespacePrefix((String)entry.getKey()), entry.getValue());

        }
        return props;
    }

    /**
     * Will create a map of properties where the names have a prefix
     * 
     * @param props the source set of properties
     * @param prefix the prefix to filter on
     * @return and new Map containing the filtered list of properties or an empty map
     *         if no properties matched the prefix
     * @deprecated use void getPropertiesWithPrefix(Map props, String prefix, Map
     *             newProps)
     */
    public static Map getPropertiesWithPrefix(Map props, String prefix)
    {
        Map newProps = new HashMap();

        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            if (entry.getKey().toString().startsWith(prefix))
            {
                newProps.put(entry.getKey(), entry.getValue());
            }
        }
        return newProps;
    }

    /**
     * Will create a map of properties where the names have a prefix Allows the
     * callee to supply the target map so a comarator can be set
     * 
     * @param props the source set of properties
     * @param prefix the prefix to filter on
     * @param newProps return map containing the filtered list of properties or an
     *            empty map if no properties matched the prefix
     */
    public static void getPropertiesWithPrefix(Map props, String prefix, Map newProps)
    {
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            if (entry.getKey().toString().startsWith(prefix))
            {
                newProps.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static Map getPropertiesWithoutPrefix(Map props, String prefix)
    {
        Map newProps = new HashMap();
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            if (!entry.getKey().toString().startsWith(prefix))
            {
                newProps.put(entry.getKey(), entry.getValue());
            }
        }
        return newProps;
    }

    public static Properties getPropertiesFromQueryString(String query)
    {
        Properties props = new Properties();

        if (query == null)
        {
            return props;
        }

        query = new StringBuffer(query.length() + 1).append('&').append(query).toString();

        int x = 0;
        while ((x = addProperty(query, x, props)) != -1)
        {
            // run
        }

        return props;
    }

    private static int addProperty(String query, int start, Properties properties)
    {
        int i = query.indexOf('&', start);
        int i2 = query.indexOf('&', i + 1);
        String pair;
        if (i > -1 && i2 > -1)
        {
            pair = query.substring(i + 1, i2);
        }
        else if (i > -1)
        {
            pair = query.substring(i + 1);
        }
        else
        {
            return -1;
        }
        int eq = pair.indexOf('=');

        if (eq <= 0)
        {
            String key = pair;
            String value = StringUtils.EMPTY;
            properties.setProperty(key, value);
        }
        else
        {
            String key = pair.substring(0, eq);
            String value = (eq == pair.length() ? StringUtils.EMPTY : pair.substring(eq + 1));
            properties.setProperty(key, value);
        }
        return i2;
    }

    /**
     * @deprecated Use {@link MapUtils#toString(Map, boolean)} instead
     */
    public static String propertiesToString(Map props, boolean newline)
    {
        return MapUtils.toString(props, newline);
    }

}
