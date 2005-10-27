/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.util;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * <code>PropertiesHelper</code> is a utility class for manipulating and
 * filtering property Maps.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PropertiesHelper
{

    /**
     * This method reads the file in to a java.util.Properties object and
     * returns it.
     */
    public static synchronized java.util.Properties loadProperties(String fileName) throws Exception
    {
        java.util.Properties p = null;
        try {
            FileInputStream is = new FileInputStream(fileName);
            p = new java.util.Properties();
            p.load(is);
        } catch (Exception ex) {
            throw ex;
        }
        return p;
    }

    public static String removeXmlNamespacePrefix(String eleName)
    {
        int i = eleName.indexOf(":");
        return eleName.substring(i + 1, eleName.length());
    }

    public static String removeNamespacePrefix(String eleName)
    {
        int i = eleName.lastIndexOf(".");
        return eleName.substring(i + 1, eleName.length());
    }

    public static Map removeNamspaces(Map properties)
    {
        HashMap props = new HashMap(properties.size());
        Map.Entry entry;
        for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
            entry = (Map.Entry) iter.next();
            props.put(removeNamespacePrefix((String) entry.getKey()), entry.getValue());

        }
        return props;
    }

    public static Object getProperty(Map props, Object key, Object defaultValue)
    {
        if (props == null) {
            return defaultValue;
        }
        Object temp = props.get(key);
        if (temp == null) {
            temp = defaultValue;
        }
        return temp;
    }

    public static String getStringProperty(Map props, Object key, String defaultValue)
    {
        if (props == null) {
            return defaultValue;
        }
        Object temp = props.get(key);
        if (temp == null) {
            temp = defaultValue;
        }
        return (temp == null ? null : temp.toString());
    }

    public static int getIntProperty(Map props, Object key, int defaultValue)
    {
        if (props == null) {
            return defaultValue;
        }
        int ret = defaultValue;
        Object temp = props.get(key);
        if (temp != null) {
            try {
                ret = Integer.parseInt(temp.toString());
            } catch (NumberFormatException e) {
                ret = defaultValue;
            }
        }
        return ret;
    }

    public static long getLongProperty(Map props, Object key, long defaultValue)
    {
        if (props == null) {
            return defaultValue;
        }
        long ret = defaultValue;
        Object temp = props.get(key);
        if (temp != null) {
            try {
                ret = Long.parseLong(temp.toString());
            } catch (NumberFormatException e) {
                ret = defaultValue;
            }
        }
        return ret;
    }

    public static double getDoubleProperty(Map props, Object key, double defaultValue)
    {
        if (props == null) {
            return defaultValue;
        }
        double ret = defaultValue;
        Object temp = props.get(key);
        if (temp != null) {
            try {
                ret = Double.parseDouble(temp.toString());
            } catch (NumberFormatException e) {
                ret = defaultValue;
            }
        }
        return ret;
    }

    public static boolean getBooleanProperty(Map props, Object key, boolean defaultValue)
    {
        if (props == null) {
            return defaultValue;
        }
        boolean ret = defaultValue;
        Object temp = props.get(key);
        if (temp != null) {
            try {
                ret = Boolean.valueOf(temp.toString()).booleanValue();
            } catch (NumberFormatException e) {
                ret = defaultValue;
            }
        }
        return ret;
    }

    public static Map reverseProperties(Map props)
    {
        Map newProps = new HashMap();
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            newProps.put(entry.getValue(), entry.getKey());
        }
        return newProps;
    }

    public static int getIntValue(Object value)
    {
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    public static long getLongValue(Object value)
    {
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    public static boolean getBooleanValue(Object value)
    {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return Boolean.valueOf(value.toString()).booleanValue();
    }

    public static Map getPropertiesWithPrefix(Map props, String prefix)
    {
        Map newProps = new HashMap();
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            if (entry.getKey().toString().startsWith(prefix)) {
                newProps.put(entry.getKey(), entry.getValue());
            }
        }
        return newProps;
    }

    public static Map getPropertiesWithoutPrefix(Map props, String prefix)
    {
        Map newProps = new HashMap();
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            if (!entry.getKey().toString().startsWith(prefix)) {
                newProps.put(entry.getKey(), entry.getValue());
            }
        }
        return newProps;
    }

    public static Properties getPropertiesFromQueryString(String query)
    {
        Properties props = new Properties();
        if (query == null) {
            return props;
        }
        query = "&" + query;
        int x = 0;
        while ((x = addProperty(query, x, props)) != -1) {
        }

        return props;
    }

    private static int addProperty(String query, int start, Properties properties)
    {
        int i = query.indexOf("&", start);
        int i2 = query.indexOf("&", i + 1);
        String pair;
        if (i > -1 && i2 > -1) {
            pair = query.substring(i + 1, i2);
        } else if (i > -1) {
            pair = query.substring(i + 1);
        } else {
            return -1;
        }
        int eq = pair.indexOf("=");

        if (eq <= 0) {
            String key = pair;
            String value = "";
            properties.setProperty(key, value);
        } else {
            String key = pair.substring(0, eq);
            String value = (eq == pair.length() ? "" : pair.substring(eq + 1));
            properties.setProperty(key, value);
        }
        return i2;
    }

    public static String propertiesToString(Map props, boolean newline)
    {
        StringBuffer buf = new StringBuffer(props.size()*32);
        buf.append("{");

        if (props == null || props.isEmpty()) {
            buf.append("}");
            return buf.toString();
        }

        if (newline) {
            buf.append(Utility.CRLF);
        }

        Object[] entries = props.entrySet().toArray();
        int i, numEntries = entries.length;
        for (i = 0; i < numEntries - 1; i++) {
			appendMaskedProperty(buf, (Map.Entry)entries[i]);
			if (newline) {
				buf.append(Utility.CRLF);
			}
			else {
				buf.append(", ");
			}
		}

        // don't forget the last one
		appendMaskedProperty(buf, (Map.Entry)entries[i]);

		if (newline) {
			buf.append(Utility.CRLF);
		}

        buf.append("}");
        return buf.toString();
    }
    
    private static void appendMaskedProperty(StringBuffer buffer, Map.Entry property) {
    	String key = property.getKey().toString();
		buffer.append(key).append("=");
		if (key.equalsIgnoreCase("password")) {
			buffer.append("*****");
		}
		else {
			buffer.append(property.getValue());
		}
    }
}
