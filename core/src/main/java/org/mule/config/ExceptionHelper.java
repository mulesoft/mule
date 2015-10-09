/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ExceptionReader;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.registry.ServiceType;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.util.ClassUtils;
import org.mule.util.MapUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.SpiUtils;
import org.mule.util.SystemUtils;

/**
 * <code>ExceptionHelper</code> provides a number of helper functions that can be
 * useful for dealing with Mule exceptions. This class has 3 core functions - <p/> 1.
 * ErrorCode lookup. A corresponding Mule error code can be found using for a given
 * Mule exception 2. Additional Error information such as Java doc url for a given
 * exception can be resolved using this class 3. Error code mappings can be looked up
 * by providing the the protocol to map to and the Mule exception.
 */

public final class ExceptionHelper
{
    private static final String MULE_PACKAGE_REGEXP = "(?:org|com)\\.mule(?:soft)?\\.(?!mvel2)(?!el).*";

    /**
     * This is the property to set the error code to no the message it is the
     * property name the Transport provider uses set the set the error code on the
     * underlying message
     */
    public static final String ERROR_CODE_PROPERTY = "error.code.property";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ExceptionHelper.class);

    private static String J2SE_VERSION = "";

    /**
     * todo How do you get the j2ee version??
     */
    private static final String J2EE_VERSION = "1.3ee";

    private static Properties errorDocs = new Properties();
    private static Properties errorCodes = new Properties();
    private static Map reverseErrorCodes = null;
    private static Map<String,Properties> errorMappings = new HashMap<String,Properties>();
    private static Map<String,Boolean> disposeListenerRegistered = new HashMap<String,Boolean>();

    private static final int EXCEPTION_THRESHOLD = 3;
    private static boolean verbose = true;

    private static boolean initialised = false;

    /**
     * A list of the exception readers to use for different types of exceptions
     */
    private static List<ExceptionReader> exceptionReaders = new ArrayList<ExceptionReader>();

    /**
     * The default ExceptionReader which will be used for most types of exceptions
     */
    private static ExceptionReader defaultExceptionReader = new DefaultExceptionReader();

    static
    {
        initialise();
    }

    /**
     * Do not instanciate.
     */
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

            String name = SpiUtils.SERVICE_ROOT + ServiceType.EXCEPTION.getPath()
                    + "/mule-exception-codes.properties";
            InputStream in = ExceptionHelper.class.getClassLoader().getResourceAsStream(name);
            if (in == null)
            {
                throw new IllegalArgumentException("Failed to load resource: " + name);
            }
            errorCodes.load(in);
            in.close();

            reverseErrorCodes = MapUtils.invertMap(errorCodes);

            name = SpiUtils.SERVICE_ROOT + ServiceType.EXCEPTION.getPath()
                    + "/mule-exception-config.properties";
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
            return (Class) clazz;
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
            return (Class) clazz;
        }
    }

    private static Properties getErrorMappings(String protocol, final MuleContext muleContext)
    {
        Properties m = errorMappings.get(getErrorMappingCacheKey(protocol,muleContext));
        if (m != null)
        {
            return m;
        }
        else
        {
            String name = SpiUtils.SERVICE_ROOT + ServiceType.EXCEPTION.getPath() + "/" + protocol + "-exception-mappings.properties";
            Properties p = PropertiesUtils.loadAllProperties(name, muleContext.getExecutionClassLoader());
            errorMappings.put(getErrorMappingCacheKey(protocol, muleContext), p);
            registerAppDisposeListener(muleContext);
            return p;
        }
    }

    private static void registerAppDisposeListener(MuleContext muleContext)
    {
        if (!disposeListenerRegistered.containsKey(muleContext.getConfiguration().getId()))
        {
            try
            {
                muleContext.registerListener(createClearCacheListenerOnContextDispose(muleContext));
                disposeListenerRegistered.put(muleContext.getConfiguration().getId(),true);
            }
            catch (NotificationException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
    }

    private static MuleContextNotificationListener<MuleContextNotification> createClearCacheListenerOnContextDispose(final MuleContext muleContext)
    {
        return new MuleContextNotificationListener<MuleContextNotification>() {
            @Override
            public void onNotification(MuleContextNotification notification)
            {
                if (notification.getAction() == MuleContextNotification.CONTEXT_DISPOSED)
                {
                    clearCacheFor(muleContext);
                    disposeListenerRegistered.remove(notification.getMuleContext().getConfiguration().getId());
                }
            }
        };
    }

    private static String getErrorMappingCacheKey(String protocol, MuleContext muleContext)
    {
        return protocol + "-" + muleContext.getConfiguration().getId();
    }

    public static String getErrorCodePropertyName(String protocol, MuleContext muleContext)
    {
        protocol = protocol.toLowerCase();
        Properties mappings = getErrorMappings(protocol,muleContext);
        if (mappings == null)
        {
            return null;
        }
        return mappings.getProperty(ERROR_CODE_PROPERTY);
    }

    /**
     * Maps an exception thrown for a certain protocol to an error.
     * When there's no specific error for such transport it will return a generic error.
     * Most likely the returned error is an integer code.
     *
     * @param protocol scheme for the transport
     * @param exception exception mapped to error
     * @param muleContext the application context
     * @return the error for exception for the specific protocol
     */
    public static String getErrorMapping(String protocol, Class exception, MuleContext muleContext)
    {
        String code = getTransportErrorMapping(protocol, exception, muleContext);
        if (code != null)
        {
            return code;
        }
        code = String.valueOf(getErrorCode(exception));
        // Finally lookup mapping based on error code and return the Mule error
        // code if a match is not found
        return getErrorMappings(protocol, muleContext).getProperty(code, code);
    }

    /**
     *
     * Maps an exception thrown for a certain protocol to an error.
     * Most likely the returned error is an integer code.
     *
     * @param protocol scheme for the transport
     * @param exception exception mapped to error
     * @param muleContext the application context
     * @return the error for exception for the specific protocol
     */
    public static String getTransportErrorMapping(String protocol, Class exception, MuleContext muleContext)
    {
        protocol = protocol.toLowerCase();
        Properties mappings = getErrorMappings(protocol, muleContext);
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
        return null;
    }

    /**
     * @deprecated since 3.8.0
     */
    @Deprecated
    public static String getJavaDocUrl(Class<?> exception)
    {
        return getDocUrl("javadoc.", exception.getName());
    }

    /**
     * @deprecated since 3.8.0
     */
    @Deprecated
    public static String getDocUrl(Class<?> exception)
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

        return DefaultMuleConfiguration.fullStackTraces ? root : sanitize(root);
    }

    public static Throwable sanitizeIfNeeded(Throwable t)
    {
        return DefaultMuleConfiguration.fullStackTraces ? t : sanitize(t);
    }

    /**
     * Removes some internal Mule entries from the stacktrace. Modifies the
     * passed-in throwable stacktrace.
     */
    public static Throwable sanitize(Throwable t)
    {
        if (t == null)
        {
            return null;
        }
        StackTraceElement[] trace = t.getStackTrace();
        List<StackTraceElement> newTrace = new ArrayList<StackTraceElement>();
        for (StackTraceElement stackTraceElement : trace)
        {
            if (!isMuleInternalClass(stackTraceElement.getClassName()))
            {
                newTrace.add(stackTraceElement);
            }
        }

        StackTraceElement[] clean = new StackTraceElement[newTrace.size()];
        newTrace.toArray(clean);
        t.setStackTrace(clean);

        Throwable cause = t.getCause();
        while (cause != null)
        {
            sanitize(cause);
            cause = cause.getCause();
        }

        return t;
    }


    /**
     * Removes some internal Mule entries from the stacktrace. Modifies the
     * passed-in throwable stacktrace.
     */
    public static Throwable summarise(Throwable t, int depth)
    {
        t = sanitize(t);
        StackTraceElement[] trace = t.getStackTrace();

        int newStackDepth = Math.min(trace.length, depth);
        StackTraceElement[] newTrace = new StackTraceElement[newStackDepth];

        System.arraycopy(trace, 0, newTrace, 0, newStackDepth);
        t.setStackTrace(newTrace);

        return t;
    }

    private static boolean isMuleInternalClass(String className)
    {
        /*
           Sacrifice the code quality for the sake of keeping things simple -
           the alternative would be to pass MuleContext into every exception constructor.
        */
        for (String mulePackage : DefaultMuleConfiguration.stackTraceFilter)
        {
            if (className.startsWith(mulePackage))
            {
                return true;
            }
        }
        return false;
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
                exception = (MuleException) cause;
            }
            final Throwable tempCause = getExceptionReader(cause).getCause(cause);
            if (DefaultMuleConfiguration.fullStackTraces)
            {
                cause = tempCause;
            }
            else
            {
                cause = ExceptionHelper.sanitize(tempCause);
            }
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause)
            {
                break;
            }
        }
        return exception;
    }

    public static List<Throwable> getExceptionsAsList(Throwable t)
    {
        List<Throwable> exceptions = new ArrayList<Throwable>();
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
        Throwable root = getRootException(t);
        MuleException rootMule = getRootMuleException(t);

        StringBuilder buf = new StringBuilder();

        ExceptionReader rootMuleReader = getExceptionReader(rootMule);
        buf.append(rootMuleReader.getMessage(rootMule))
           .append(" (")
           .append(rootMule.getClass().getName())
           .append(")")
           .append(SystemUtils.LINE_SEPARATOR);

        if (verbose)
        {
            int processedElements = 0;
            int processedMuleElements = 1;
            for (StackTraceElement stackTraceElement : root.getStackTrace())
            {
                if (processedMuleElements > EXCEPTION_THRESHOLD)
                {
                    break;
                }
                
                ++processedElements;
                if (stackTraceElement.getClassName().matches(MULE_PACKAGE_REGEXP))
                {
                    ++processedMuleElements;
                }
                
                buf.append("  ")
                   .append(stackTraceElement.getClassName())
                   .append(".")
                   .append(stackTraceElement.getMethodName())
                   .append(":")
                   .append(stackTraceElement.getLineNumber())
                   .append(")")
                   .append(SystemUtils.LINE_SEPARATOR);
            }
            
            if (root.getStackTrace().length - processedElements > 0)
            {
                buf.append("  (")
                   .append(root.getStackTrace().length - processedElements)
                   .append(" more...)")
                   .append(SystemUtils.LINE_SEPARATOR);
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

    public static <T> T traverseCauseHierarchy(Throwable e, ExceptionEvaluator<T> evaluator)
    {
        LinkedList<Throwable> exceptions = new LinkedList<Throwable>();
        exceptions.add(e);
        while (e.getCause() != null && !e.getCause().equals(e))
        {
            exceptions.addFirst(e.getCause());
            e = e.getCause();
        }
        for (Throwable exception : exceptions)
        {
            T value = evaluator.evaluate(exception);
            if (value != null)
            {
                return value;
            }
        }
        return null;
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
        for (ExceptionReader exceptionReader : exceptionReaders)
        {
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
        StringBuilder msg = new StringBuilder();
        msg.append(er.getMessage(t)).append(". Type: ").append(t.getClass());
        return msg.toString();
    }

    public static <T extends Throwable>T unwrap(T t)
    {
        if(t instanceof InvocationTargetException)
        {
            return (T)((InvocationTargetException)t).getTargetException();
        }
        return t;

    }

    public static interface ExceptionEvaluator<T>
    {
        T evaluate(Throwable e);
    }

    public static Throwable getNonMuleException(Throwable t)
    {
        if (!(t instanceof MuleException))
        {
            return t;
        }
        Throwable cause = t;
        while (cause != null)
        {
            cause = getExceptionReader(cause).getCause(cause);
            // address some misbehaving exceptions, avoid endless loop
            if (t == cause || !(cause instanceof MuleException))
            {
                break;
            }
        }
        return cause instanceof MuleException ? null : cause;
    }

    private static void clearCacheFor(MuleContext muleContext)
    {
        List<String> entriesToRemove = new ArrayList<String>();
        for (String key : errorMappings.keySet())
        {
            if (key.endsWith(muleContext.getConfiguration().getId()))
            {
                entriesToRemove.add(key);

            }
        }
        for (String key : entriesToRemove)
        {
            errorMappings.remove(key);
        }
    }
}
