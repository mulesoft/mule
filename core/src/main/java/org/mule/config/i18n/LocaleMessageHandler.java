/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>LocaleMessageHandler</code> is essentially a merging of the Message
 * and Messages classes, since there is no good reason to have them separate. A
 * key point is that this Handler is meant to be used for application-specific
 * messages, rather than core system messages. (That's not to say it couldn't
 * eventually replace the latter, however). Note that message codes are Strings
 * here, instead of the ints in Message.
 * <p/>
 * The LocaleMessageHandler can be called directly, but is really meant to be
 * called by LocaleMessage classes as done in the examples.
 * <p/>
 * Note that this class assumes the resource bundle is in the format
 * <bundle-name>-messages and is located at the top of the jar or classes
 * directory. We can later add the ability to specify a path prefix.
 */
public class LocaleMessageHandler
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(LocaleMessageHandler.class);

    /**
     * Get the resource string for the given bundle name and resource code
     */
    public static String getString(String bundleName, String code)
    {
        return getString(bundleName, code, new Object[]{});
    }

    /**
     * Get the resource string for the given bundle name, resource code and
     * one argument
     */
    public static String getString(String bundleName, String code, Object arg1)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }

        return getString(bundleName, code, new Object[]{arg1});
    }

    /**
     * Get the resource string for the given bundle name, resource code and
     * two arguments
     */
    public static String getString(String bundleName, String code, Object arg1, Object arg2)
    {
        if (arg1 == null)
        {
            arg1 = "null";
        }

        if (arg2 == null)
        {
            arg2 = "null";
        }

        return getString(bundleName, code, new Object[]{arg1, arg2});
    }

    /**
     * Get the resource string for the given bundle name, resource code and array
     * of arguments.
     */
    public static String getString(String bundleName, String code, Object[] args)
    {
        return getString(bundleName, Locale.getDefault(), code, args);
    }

    /**
     * Get the resource string for the given bundle name, locale, resource code and array
     * of arguments. All above methods invoke this one.
     */
    public static String getString(String bundleName, Locale locale, String code, Object[] args)
    {
        String path = bundleName + "-messages";
        ResourceBundle bundle = ResourceBundle.getBundle(path, locale);
        String m = bundle.getString(code);

        if (m == null)
        {
            logger.error("Failed to find message for id " + code + " in resource bundle " + path);
            return "";
        }

        return MessageFormat.format(m, args);
    }

}

