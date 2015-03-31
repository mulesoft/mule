/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class MessageFactory
{
    /**
     * Default is {@link ReloadControl.Always}.
     */
    public static final ResourceBundle.Control DEFAULT_RELOAD_CONTROL = new ReloadControl.Always();

    /**
     * This error code is used for {@link Message} instances that are not read from a
     * resource bundles but are created only with a string.
     */
    private static final int STATIC_ERROR_CODE = -1;
    private static final transient Object[] EMPTY_ARGS = new Object[]{};

    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * Do not use the default reload control to avoid loading the resource bundle upon each request.
     * Subclasses can override to provide a different default.
     */
    protected ResourceBundle.Control reloadControl = null;

    /**
     * Computes the bundle's full path 
     * (<code>META-INF/services/org/mule/i18n/&lt;bundleName&gt;-messages.properties</code>) from
     * <code>bundleName</code>.
     * 
     * @param bundleName Name of the bundle without the &quot;messages&quot; suffix and without
     *          file extension.
     */
    protected static String getBundlePath(String bundleName)
    {
        return "META-INF.services.org.mule.i18n." + bundleName + "-messages";
    }
    
    /**
     * Factory method to create a new {@link Message} instance that is filled with the formatted
     * message with id <code>code</code> from the resource bundle <code>bundlePath</code>.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     * @param arg
     * @see #getBundlePath(String)
     */
    protected Message createMessage(String bundlePath, int code, Object arg)
    {
        return createMessage(bundlePath, code, new Object[] {arg});
    }
    
    /**
     * Factory method to create a new {@link Message} instance that is filled with the formatted
     * message with id <code>code</code> from the resource bundle <code>bundlePath</code>.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     * @param arg1
     * @param arg2
     * @see #getBundlePath(String)
     */
    protected Message createMessage(String bundlePath, int code, Object arg1, Object arg2)
    {
        return createMessage(bundlePath, code, new Object[] {arg1, arg2});
    }
    
    /**
     * Factory method to create a new {@link Message} instance that is filled with the formatted
     * message with id <code>code</code> from the resource bundle <code>bundlePath</code>.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     * @param arg1
     * @param arg2
     * @param arg3
     * @see #getBundlePath(String)
     */
    protected Message createMessage(String bundlePath, int code, Object arg1, Object arg2, 
        Object arg3)
    {
        return createMessage(bundlePath, code, new Object[] {arg1, arg2, arg3});
    }
    
    /**
     * Factory method to create a new {@link Message} instance that is filled with the formatted
     * message with id <code>code</code> from the resource bundle <code>bundlePath</code>.
     * 
     * <b>Attention:</b> do not confuse this method with 
     * <code>createMessage(String, int, Object)</code>.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     * @param arguments
     * @see #getBundlePath(String)
     */
    protected Message createMessage(String bundlePath, int code, Object... arguments)
    {
        String messageString = getString(bundlePath, code, arguments);
        return new Message(messageString, code, arguments);
    }

    /**
     * Factory method to create a new {@link Message} instance that is filled with the formatted
     * message with id <code>code</code> from the resource bundle <code>bundlePath</code>.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     */
    protected Message createMessage(String bundlePath, int code)
    {
        String messageString = getString(bundlePath, code, null);
        return new Message(messageString, code, EMPTY_ARGS);
    }

    /**
     * Factory method to create a {@link Message} instance that is not read from a resource bundle.
     *
     * @param message Message's message text
     * @return a Messsage instance that has an error code of -1 and no arguments.
     */
    public static Message createStaticMessage(String message)
    {
        return new Message(message, STATIC_ERROR_CODE, EMPTY_ARGS);
    }

    /**
     * Factory method to create a {@link Message} instance that is not read from a resource bundle.
     *
     * @param message Static message text that may contain format specifiers
     * @param arguments Arguments referenced by the format specifiers in the message string.
     * @return a Messsage instance that has an error code of -1 and no arguments.
     */
    public static Message createStaticMessage(String message, Object... arguments)
    {
        return new Message(String.format(message, arguments), STATIC_ERROR_CODE, EMPTY_ARGS);
    }

    /**
     * Factory method to read the message with code <code>code</code> from the resource bundle.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     * @return formatted error message as {@link String}
     */
    protected String getString(String bundlePath, int code)
    {
        return getString(bundlePath, code, null);
    }
    
    /**
     * Factory method to read the message with code <code>code</code> from the resource bundle.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     * @param arg
     * @return formatted error message as {@link String}
     */
    protected String getString(String bundlePath, int code, Object arg)
    {
        Object[] arguments = new Object[] {arg};
        return getString(bundlePath, code, arguments);
    }
    
    /**
     * Factory method to read the message with code <code>code</code> from the resource bundle.
     * 
     * @param bundlePath complete path to the resource bundle for lookup
     * @param code numeric code of the message
     * @param arg1
     * @param arg2
     * @return formatted error message as {@link String}
     */
    protected String getString(String bundlePath, int code, Object arg1, Object arg2)
    {
        Object[] arguments = new Object[] {arg1, arg2};
        return getString(bundlePath, code, arguments);
    }

    protected String getString(String bundlePath, int code, Object[] args)
    {
        // We will throw a MissingResourceException if the bundle name is invalid
        // This happens if the code references a bundle name that just doesn't exist
        ResourceBundle bundle = getBundle(bundlePath);

        try
        {
            String m = bundle.getString(String.valueOf(code));
            if (m == null)
            {
                logger.error("Failed to find message for id " + code + " in resource bundle " + bundlePath);
                return "";
            }

            return MessageFormat.format(m, args);
        }
        catch (MissingResourceException e)
        {
            logger.error("Failed to find message for id " + code + " in resource bundle " + bundlePath);
            return "";
        }
    }

    /**
     * @throws MissingResourceException if resource is missing
     */
    protected ResourceBundle getBundle(String bundlePath)
    {
        Locale locale = Locale.getDefault();
        if (logger.isTraceEnabled())
        {
            logger.trace("Loading resource bundle: " + bundlePath + " for locale " + locale);
        }
        final ResourceBundle.Control control = getReloadControl();
        ResourceBundle bundle = control != null
                                            ? ResourceBundle.getBundle(bundlePath, locale, getClassLoader(), control)
                                            : ResourceBundle.getBundle(bundlePath, locale, getClassLoader());

        return bundle;
    }

    /**
     * Override this method to return the classloader for the bundle/module which 
     * contains the needed resource files.
     */
    protected ClassLoader getClassLoader()
    {
        final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        // if there's a deployment classloader present, use it for finding resources
        return ccl == null ? getClass().getClassLoader() : ccl;
    }

    /**
     * Subclasses should override to customize the bundle reload control. Implementations must
     * save the instance in a field for stateful reload control. Return null to fallback to
     * default JVM behavior (permanent cache).
     *
     * @see #DEFAULT_RELOAD_CONTROL
     */
    protected ResourceBundle.Control getReloadControl()
    {
        return reloadControl;
    }
}


