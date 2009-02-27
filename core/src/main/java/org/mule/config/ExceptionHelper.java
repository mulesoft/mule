/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ExceptionReader;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.util.MapUtils;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ExceptionHelper</code> provides a number of helper functions that can be
 * useful for dealing with Mule exceptions. This class has 3 core functions - <p/> 1.
 * ErrorCode lookup. A corresponding Mule error code can be found using for a given
 * Mule exception 2. Addtional Error information such as Java doc url for a given
 * exception can be resolved using this class 3. Error code mappings can be looked up
 * by providing the the protocol to map to and the Mule exception.
 */

public final class ExceptionHelper
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

    private static String J2SE_VERSION = "";

    /** todo How do you get the j2ee version?? */
    private static final String J2EE_VERSION = "1.3ee";

    private static Properties errorDocs = new Properties();
    private static Properties errorCodes = new Properties();
    private static Map reverseErrorCodes = null;
    private static Map errorMappings = new HashMap();

    private static int exceptionThreshold = 0;
    private static boolean verbose = true;

    private static boolean initialised = false;

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

    /** Do not instanciate. */
    private ExceptionHelper()
    {
        // no-op
    }

    private static void initialise()
    {
        try
        {
            if (initialised)
            {
                return;
            }

            registerExceptionReader(new MuleExceptionReader());
            registerExceptionReader(new NamingExceptionReader());
            J2SE_VERSION = System.getProperty("java.specification.version");

            String name = SpiUtils.SERVICE_ROOT + SpiUtils.EXCEPTION_SERVICE_PATH
                          + "mule-exception-codes.properties";
            InputStream in = ExceptionHelper.class.getClassLoader().getResourceAsStream(name);
            if (in == null)
            {
                throw new IllegalArgumentException("Failed to load resource: " + name);
            }
            errorCodes.load(in);
            in.close();

            reverseErrorCodes = MapUtils.invertMap(errorCodes);

            name = SpiUtils.SERVICE_ROOT + SpiUtils.EXCEPTION_SERVICE_PATH
                   + "mule-exception-config.properties";
            in = ExceptionHelper.class.getClassLoader().getResourceAsStream(name);
            if (in == null)
            {
                throw new IllegalArgumentException("Failed to load resource: " + name);
            }
            errorDocs.load(in);
            in.close();

            initialised = true;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToLoad("Exception resources"), e);
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
            String name = SpiUtils.SERVICE_ROOT + SpiUtils.EXCEPTION_SERVICE_PATH + protocol + "-exception-mappings.properties";
            InputStream in = ExceptionHelper.class.getClassLoader().getResourceAsStream(name);
            if (in == null)
            {
                errorMappings.put(protocol, "not found");
                logger.warn("Failed to load error mappings from: " + name
                            + " This may be because there are no error code mappings for protocol: "
                            + protocol);
                return null;
            }

            Properties p = new Properties();
            try
            {
                p.load(in);
                in.close();
            }
            catch (IOException iox)
            {
                throw new IllegalArgumentException("Failed to load resource: " + name);
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

    public static MuleException getRootMuleException(Throwable t)
    {
        Throwable cause = t;
        MuleException exception = null;
        while (cause != null)
        {
            if (cause instanceof MuleException)
            {
                exception = (MuleException)cause;
            }
            cause = getExceptionReader(cause).getCause(cause);
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause)
            {
                break;
            }
        }
        return exception;
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
