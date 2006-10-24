/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.i18n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * <code>Messages</code> provides facilities for constructing <code>Message</code>
 * objects and access to core message constants.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Messages implements CoreMessageConstants
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(Messages.class);

    public static final String DEFAULT_BUNDLE = "core";

    private static Map bundles = new HashMap();

    private static Object[] emptyArgs = new Object[]{};

    public static String get(int code)
    {
        return getString(DEFAULT_BUNDLE, code, emptyArgs);
    }

    public static String get(int code, Object[] args)
    {
        if (args == null)
        {
            args = Messages.emptyArgs;
        }
        return getString(DEFAULT_BUNDLE, code, args);
    }

    public static String get(int code, Object arg1)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }
        return getString(DEFAULT_BUNDLE, code, new Object[]{arg1});
    }

    public static String get(int code, Object arg1, Object arg2)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }
        if (arg2 == null)
        {
            arg2 = "null";
        }
        return getString(DEFAULT_BUNDLE, code, new Object[]{arg1, arg2});
    }

    public static String get(int code, Object arg1, Object arg2, Object arg3)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }
        if (arg2 == null)
        {
            arg2 = "null";
        }
        if (arg3 == null)
        {
            arg3 = "null";
        }
        return getString(DEFAULT_BUNDLE, code, new Object[]{arg1, arg2, arg3});
    }

    public static String get(String bundle, int code)
    {
        return getString(bundle, code, emptyArgs);
    }

    public static String get(String bundle, int code, Object[] args)
    {
        if (args == null)
        {
            args = Messages.emptyArgs;
        }
        return getString(bundle, code, args);
    }

    public static String get(String bundle, int code, Object arg1)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }
        return getString(bundle, code, new Object[]{arg1});
    }

    public static String get(String bundle, int code, Object arg1, Object arg2)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }
        if (arg2 == null)
        {
            arg2 = "null";
        }
        return getString(bundle, code, new Object[]{arg1, arg2});
    }

    public static String get(String bundle, int code, Object arg1, Object arg2, Object arg3)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }
        if (arg2 == null)
        {
            arg2 = "null";
        }
        if (arg3 == null)
        {
            arg3 = "null";
        }
        return getString(bundle, code, new Object[]{arg1, arg2, arg3});
    }

    public static String getString(String bundle, int code, Object[] args)
    {
        String m = getBundle(bundle).getString(String.valueOf(code));
        if (m == null)
        {
            logger.error("Failed to find message for id " + code + " in resource bundle " + bundle);
            return "";
        }
        return MessageFormat.format(m, args);
    }

    protected static ResourceBundle getBundle(String name)
    {
        ResourceBundle bundle = (ResourceBundle)bundles.get(name);
        if (bundle == null)
        {
            String path = "META-INF.services.org.mule.i18n." + name + "-messages";
            logger.debug("Loading resource bundle: " + path);
            Locale locale = Locale.getDefault();
            try
            {
                bundle = ResourceBundle.getBundle(path, locale);
            }
            catch (MissingResourceException e)
            {
                logger.warn("Failed to find resource bundle using default Locale: " + locale.toString()
                            + ", defaulting to Locale.US. Error was: " + e.getMessage());
                bundle = ResourceBundle.getBundle(path);
            }
            bundles.put(name, bundle);
        }
        return bundle;
    }
}
