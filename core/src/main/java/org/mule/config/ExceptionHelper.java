/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;
import org.mule.util.ClassUtils;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ExceptionHelper</code> provides a number of helper functions that can be
 * useful for dealing with Mule exceptions. This class has 3 core functions - <p/>
 * 1. ErrorCode lookup. A corresponding Mule error code can be found using for a
 * given Mule exception 2. Addtional Error information such as Java doc url for a
 * given exception can be resolved using this class 3. Error code mappings can be
 * looked up by providing the the protocol to map to and the Mule exception.
 */

public class ExceptionHelper
{
    /**
     * This is the property to set the error code to no the message it is the
     * property name the Transport provider uses set the set the error code on the
     * underlying message
     */
    public static final String ERROR_CODE_PROPERTY = "error.code.property";

    /**
     * a comma-separated list of other protocols the mappings in a file can be
     * applied to
     */
    public static final String APPLY_TO_PROPERTY = "apply.to";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ExceptionHelper.class);

    private static Properties errorDocs = new Properties();
    private static Properties errorCodes = new Properties();
    private static Map reverseErrorCodes = null;
    private static Map errorMappings = new HashMap();

    private static int exceptionThreshold = 0;
    private static boolean verbose = true;

    private static boolean initialised = false;

    private static String J2SE_VERSION = "";

    /** todo How do you get the j2ee version?? */
    private static String J2EE_VERSION = "1.3ee";

    /**
     * A list of the exception readers to use for different types of exceptions
     */
    private static List exceptionReaders = new ArrayList();

    /**
     * The default ExceptionReader which will be used for most types of exceptions
     */
    private static ExceptionReader defaultExceptionReader = new DefaultExceptionReader();

    static
    {
        initialise();
    }

    public static void initialise()
    {
        try
        {
            if (initialised)
            {
                return;
            }

            registerExceptionReader(new UMOExceptionReader());
            registerExceptionReader(new NamingExceptionReader());
            J2SE_VERSION = System.getProperty("java.specification.version");

            InputStream is = SpiUtils.findServiceDescriptor("org/mule/config",
                "mule-exception-codes.properties", ExceptionHelper.class);
            if (is == null)
            {
                throw new NullPointerException(
                    "Failed to load resource: META_INF/services/org/mule/config/mule-exception-codes.properties");
            }
            errorCodes.load(is);
            reverseErrorCodes = MapUtils.invertMap(errorCodes);
            is = SpiUtils.findServiceDescriptor("org/mule/config", "mule-exception-config.properties",
                ExceptionHelper.class);
            if (is == null)
            {
                throw new NullPointerException(
                    "Failed to load resource: META_INF/services/org/mule/config/mule-exception-config.properties");
            }
            errorDocs.load(is);

            initialised = true;
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(Message.createStaticMessage("Failed to load Exception resources"),
                e);
        }
    }

    public static int getErrorCode(Class exception)
    {
        String code = errorCodes.getProperty(exception.getName(), "-1");
        return Integer.parseInt(code);
    }

    public static Class getErrorClass(int code)
    {
        String key = String.valueOf(code);
        Object clazz = reverseErrorCodes.get(key);
        if (clazz == null)
        {
            return null;
        }
        else if (clazz instanceof Class)
        {
            return (Class)clazz;
        }
        else
        {
            try
            {
                clazz = ClassUtils.loadClass(clazz.toString(), ExceptionHelper.class);
            }
            catch (ClassNotFoundException e)
            {
                logger.error(e.getMessage(), e);
                return null;
            }
            reverseErrorCodes.put(key, clazz);
            return (Class)clazz;
        }
    }

    public static String getErrorMapping(String protocol, int code)
    {
        Class c = getErrorClass(code);
        if (c != null)
        {
            return getErrorMapping(protocol, c);
        }
        else
        {
            logger.error("Class not known for code: " + code);
            return "-1";
        }
    }

    private static Properties getErrorMappings(String protocol)
    {
        Object m = errorMappings.get(protocol);
        if (m != null)
        {
            if (m instanceof Properties)
            {
                return (Properties)m;
            }
            else
            {
                return null;
            }
        }
        else
        {
            InputStream is = SpiUtils.findServiceDescriptor("org/mule/config",
                protocol + "-exception-mappings.properties", ExceptionHelper.class);
            if (is == null)
            {
                errorMappings.put(protocol, "not found");
                logger.warn("Failed to load error mappings from: META-INF/services/org/mule/config/"
                            + protocol
                            + "-exception-mappings.properties. This may be because there are no error code mappings for protocol: "
                            + protocol);
                return null;
            }
            Properties p = new Properties();
            try
            {
                p.load(is);
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(
                    Message.createStaticMessage("Failed to load Exception resources"), e);
            }
            errorMappings.put(protocol, p);
            String applyTo = p.getProperty(APPLY_TO_PROPERTY, null);
            if (applyTo != null)
            {
                String[] protocols = StringUtils.splitAndTrim(applyTo, ",");
                for (int i = 0; i < protocols.length; i++)
                {
                    errorMappings.put(protocols[i], p);
                }
            }
            return p;
        }
    }

    public static String getErrorCodePropertyName(String protocol)
    {
        protocol = protocol.toLowerCase();
        Properties mappings = getErrorMappings(protocol);
        if (mappings == null)
        {
            return null;
        }
        return mappings.getProperty(ERROR_CODE_PROPERTY);
    }

    public static String getErrorMapping(String protocol, Class exception)
    {
        protocol = protocol.toLowerCase();
        Properties mappings = getErrorMappings(protocol);
        if (mappings == null)
        {
            logger.info("No mappings found for protocol: " + protocol);
            return String.valueOf(getErrorCode(exception));
        }

        Class clazz = exception;
        String code = null;
        while (!clazz.equals(Object.class))
        {
            code = mappings.getProperty(clazz.getName());
            if (code == null)
            {
                clazz = clazz.getSuperclass();
            }
            else
            {
                return code;
            }
        }
        code = String.valueOf(getErrorCode(exception));
        // Finally lookup mapping based on error code and return the Mule error
        // code if a match is not found
        return mappings.getProperty(code, code);
    }

    public static String getJavaDocUrl(Class exception)
    {
        return getDocUrl("javadoc.", exception.getName());
    }

    public static String getDocUrl(Class exception)
    {
        return getDocUrl("doc.", exception.getName());
    }

    private static String getDocUrl(String prefix, String packageName)
    {
        String key = prefix;
        if (packageName.startsWith("java.") || packageName.startsWith("javax."))
        {
            key += J2SE_VERSION;
        }
        String url = getUrl(key, packageName);
        if (url == null && (packageName.startsWith("java.") || packageName.startsWith("javax.")))
        {
            key = prefix + J2EE_VERSION;
            url = getUrl(key, packageName);
        }
        if (url != null)
        {
            if (!url.endsWith("/"))
            {
                url += "/";
            }
            String s = packageName.replaceAll("[.]", "/");
            s += ".html";
            url += s;
        }

        /*
         * If the exception is actually expected to occur by the calling code, the
         * following output becomes misleading when reading the logs. if
         * (logger.isDebugEnabled()) { if ("javadoc".equalsIgnoreCase(prefix)) {
         * logger.debug("Javadoc Url for package '" + packageName + "' is: " + url); }
         * else if ("doc".equalsIgnoreCase(prefix)) { logger.debug("Online Doc Url
         * for package '" + packageName + "' is: " + url); } else {
         * logger.debug(prefix + " Url for package '" + packageName + "' is: " +
         * url); } }
         */
        return url;
    }

    private static String getUrl(String key, String packageName)
    {
        String url = null;
        if (!key.endsWith("."))
        {
            key += ".";
        }
        while (packageName.length() > 0)
        {
            url = errorDocs.getProperty(key + packageName, null);
            if (url == null)
            {
                int i = packageName.lastIndexOf(".");
                if (i == -1)
                {
                    packageName = "";
                }
                else
                {
                    packageName = packageName.substring(0, i);
                }
            }
            else
            {
                break;
            }
        }
        return url;
    }

    public static Throwable getRootException(Throwable t)
    {
        Throwable cause = t;
        Throwable root = null;
        while (cause != null)
        {
            root = cause;
            cause = getExceptionReader(cause).getCause(cause);
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause)
            {
                break;
            }
        }
        return root;
    }

    public static Throwable getRootParentException(Throwable t)
    {
        Throwable cause = t;
        Throwable parent = t;
        while (cause != null)
        {
            if (cause.getCause() == null)
            {
                return parent;
            }
            parent = cause;
            cause = getExceptionReader(cause).getCause(cause);
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause)
            {
                break;
            }
        }
        return t;
    }

    public static UMOException getRootMuleException(Throwable t)
    {
        Throwable cause = t;
        UMOException umoException = null;
        while (cause != null)
        {
            if (cause instanceof UMOException)
            {
                umoException = (UMOException)cause;
            }
            cause = getExceptionReader(cause).getCause(cause);
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause)
            {
                break;
            }
        }
        return umoException;
    }

    public static List getExceptionsAsList(Throwable t)
    {
        List exceptions = new ArrayList();
        Throwable cause = t;
        while (cause != null)
        {
            exceptions.add(0, cause);
            cause = getExceptionReader(cause).getCause(cause);
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause)
            {
                break;
            }
        }
        return exceptions;
    }

    public static Map getExceptionInfo(Throwable t)
    {
        Map info = new HashMap();
        Throwable cause = t;
        while (cause != null)
        {
            info.putAll(getExceptionReader(cause).getInfo(cause));
            cause = getExceptionReader(cause).getCause(cause);
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause)
            {
                break;
            }
        }
        return info;
    }

    public static String getExceptionStack(Throwable t)
    {
        StringBuffer buf = new StringBuffer();
        // get exception stack
        List exceptions = getExceptionsAsList(t);

        int i = 1;
        for (Iterator iterator = exceptions.iterator(); iterator.hasNext(); i++)
        {
            if (i > exceptionThreshold && exceptionThreshold > 0)
            {
                buf.append("(").append(exceptions.size() - i + 1).append(" more...)");
                break;
            }
            Throwable throwable = (Throwable)iterator.next();
            ExceptionReader er = getExceptionReader(throwable);
            buf.append(i).append(". ").append(er.getMessage(throwable)).append(" (");
            buf.append(throwable.getClass().getName()).append(")\n");
            if (verbose && throwable.getStackTrace().length > 0)
            {
                StackTraceElement e = throwable.getStackTrace()[0];
                buf.append("  ")
                    .append(e.getClassName())
                    .append(":")
                    .append(e.getLineNumber())
                    .append(" (")
                    .append(getJavaDocUrl(throwable.getClass()))
                    .append(")\n");
            }
        }
        return buf.toString();
    }

    /**
     * Registers an exception reader with Mule
     *
     * @param reader the reader to register.
     */
    public static void registerExceptionReader(ExceptionReader reader)
    {
        exceptionReaders.add(reader);
    }

    /**
     * Gets an exception reader for the exception
     *
     * @param t the exception to get a reader for
     * @return either a specific reader or an instance of DefaultExceptionReader.
     *         This method never returns null;
     */
    public static ExceptionReader getExceptionReader(Throwable t)
    {
        for (Iterator iterator = exceptionReaders.iterator(); iterator.hasNext();)
        {
            ExceptionReader exceptionReader = (ExceptionReader)iterator.next();
            if (exceptionReader.getExceptionType().isInstance(t))
            {
                return exceptionReader;
            }
        }
        return defaultExceptionReader;
    }

    public static String writeException(Throwable t)
    {
        ExceptionReader er = getExceptionReader(t);
        StringBuffer msg = new StringBuffer();
        msg.append(er.getMessage(t)).append(". Type: ").append(t.getClass());
        return msg.toString();
    }
}
