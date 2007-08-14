/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 * NOTE: This class is closely based on Spring 2.0 DefaultNamespaceResolver,
 * Copyright 2002-2007 the original author or authors.  That class included
 * the attributions:
 * @author Rob Harrop
 * @author Juergen Hoeller
 *
 * NOTE: This class also contains code based on Spring 2.0 PropertiesLoaderUtils
 * Copyright 2002-2006 the original author or authors.  That class included
 * the attributions:
 * @author Juergen Hoeller
 * @author Rob Harrop
 */

package org.mule.config.spring;

import org.mule.config.spring.handlers.AbstractPriorityNamespaceHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * This is based very closely on the Spring code.  We need to re-implement it here
 * because we want to guarantee that repeated handlers for the same namespace overwrite
 * each other correctly.
 *
 * {@see org.mule.config.spring.handlers.AbstractPriorityNamespaceHandler}
 */
public class MuleNamespaceHandlerResolver implements NamespaceHandlerResolver
{

    /**
     * The location to look for the mapping files. Can be present in multiple JAR files.
     */
    private static final String SPRING_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";

    /** Logger available to subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    /** Stores the mappings from namespace URI Strings to NamespaceHandler instances */
    private Map handlerMappings;


    /**
     * Create a new <code>DefaultNamespaceHandlerResolver</code> using the
     * default mapping file location.
     * <p>This constructor will result in the thread context ClassLoader being used
     * to load resources.
     * @see #SPRING_HANDLER_MAPPINGS_LOCATION
     */
    public MuleNamespaceHandlerResolver() {
        this(null, SPRING_HANDLER_MAPPINGS_LOCATION);
    }


    /**
     * Create a new <code>DefaultNamespaceHandlerResolver</code> using the
     * default mapping file location.
     * @param classLoader the {@link ClassLoader} instance used to load mapping resources (may be <code>null</code>, in
     * which case the thread context ClassLoader will be used)
     * @see #SPRING_HANDLER_MAPPINGS_LOCATION
     */
    public MuleNamespaceHandlerResolver(ClassLoader classLoader) {
        this(classLoader, SPRING_HANDLER_MAPPINGS_LOCATION);
    }

    /**
     * Create a new <code>DefaultNamespaceHandlerResolver</code> using the
     * supplied mapping file location.
     * @param classLoader the {@link ClassLoader} instance used to load mapping resources (may be <code>null</code>, in
     * which case the thread context ClassLoader will be used)
     * @param handlerMappingsLocation the mapping file location
     * @see #SPRING_HANDLER_MAPPINGS_LOCATION
     */
    public MuleNamespaceHandlerResolver(ClassLoader classLoader, String handlerMappingsLocation) {
        Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
        ClassLoader classLoaderToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
        initHandlerMappings(classLoaderToUse, handlerMappingsLocation);
    }


    /**
     * Load the namespace URI -> <code>NamespaceHandler</code> class mappings from the configured
     * mapping file. Converts the class names into actual class instances and checks that
     * they implement the <code>NamespaceHandler</code> interface. Pre-instantiates an instance
     * of each <code>NamespaceHandler</code> and maps that instance to the corresponding
     * namespace URI.
     */
    private void initHandlerMappings(ClassLoader classLoader, String handlerMappingsLocation) {
        Collection mappings = loadMappings(classLoader, handlerMappingsLocation);
        if (logger.isDebugEnabled()) {
            logger.debug("Loaded mappings [" + mappings + "]");
        }
        this.handlerMappings = new HashMap(mappings.size());
        for (Iterator iterator = mappings.iterator(); iterator.hasNext();) {
            PropertyNameValuePair pair = (PropertyNameValuePair) iterator.next();
            String namespaceUri = pair.getName();
            String className = pair.getValue();
            try {
                Class handlerClass = ClassUtils.forName(className, classLoader);
                if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
                    throw new IllegalArgumentException("Class [" + className +
                            "] does not implement the NamespaceHandler interface");
                }
                NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
                namespaceHandler.init();
                addHandlerMapping(namespaceUri, namespaceHandler);
            }
            catch (ClassNotFoundException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ignoring namespace handler [" + className + "]: handler class not found", ex);
                }
            }
            catch (LinkageError err) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Ignoring namespace handler [" + className +
                            "]: problem with handler class file or dependent class", err);
                }
            }
        }
    }

    private Collection loadMappings(ClassLoader classLoader, String handlerMappingsLocation) {
        try {
            return loadAllProperties(handlerMappingsLocation, classLoader);
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load NamespaceHandler mappings from location [" +
                            handlerMappingsLocation + "]. Root cause: " + ex);
        }
    }

    /**
     * Locate the {@link NamespaceHandler} for the supplied namespace URI
     * from the configured mappings.
     * @param namespaceUri the relevant namespace URI
     * @return the located {@link NamespaceHandler}, or <code>null</code> if none found
     */
    public NamespaceHandler resolve(String namespaceUri) {
        return (NamespaceHandler) this.handlerMappings.get(namespaceUri);
    }

    /**
     * Allow higher priority namespace handlers to overwrite older priority handlers
     */
    private void addHandlerMapping(String namespaceUri, NamespaceHandler namespaceHandler)
    {
        if (handlerMappings.containsKey(namespaceUri))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Duplicate handlers for " + namespaceUri);
            }

            // here we just use the highest priority.
            // this isn't very elegant - it would be nicer if we could chain handlers
            // so that we use the highest priority that supports a particular element
            // (this would allow overriding and/or extension).
            // however, the NamespaceHandler interface doesn't expose any way to tell
            // whether a particular element is supported.

            NamespaceHandler previous = (NamespaceHandler) handlerMappings.get(namespaceUri);
            int previousPriority = priority(previous, namespaceUri);
            int newPriority = priority(namespaceHandler, namespaceUri);
            if (newPriority == previousPriority)
            {
                throw new IllegalStateException("Two handlers for " + namespaceUri + " of equal priority");
            }
            else if (newPriority < previousPriority)
            {
                return;
            }
        }
        this.handlerMappings.put(namespaceUri, namespaceHandler);
    }

    private int priority(NamespaceHandler namespaceHandler, String namespaceUri)
    {
        if (namespaceHandler instanceof AbstractPriorityNamespaceHandler)
        {
            return ((AbstractPriorityNamespaceHandler) namespaceHandler).getPriority();
        }
        else
        {
            throw new IllegalStateException("Multiple handlers for " + namespaceUri
                    + " (and at least one is without priority)");
        }
    }

    /**
     * A modified version of {@link org.springframework.core.io.support.PropertiesLoaderUtils#loadAllProperties(String, ClassLoader)}
     * that preserves duplicate propety names.
     *
     * @param resourceName
     * @param classLoader
     * @return
     * @throws IOException
     */
    public static Collection loadAllProperties(String resourceName, ClassLoader classLoader) throws IOException {
        Assert.notNull(resourceName, "Resource name must not be null");
        ClassLoader clToUse = classLoader;
        if (clToUse == null) {
            clToUse = ClassUtils.getDefaultClassLoader();
        }
        Collection namesAndValues = new HashSet();
        Enumeration urls = clToUse.getResources(resourceName);
        while (urls.hasMoreElements()) {
            Properties properties = new Properties();
            URL url = (URL) urls.nextElement();
            InputStream is = null;
            try {
                URLConnection con = url.openConnection();
                con.setUseCaches(false);
                is = con.getInputStream();
                properties.load(is);
            }
            finally {
                if (is != null) {
                    is.close();
                }
            }
            Enumeration names = properties.propertyNames();
            while (names.hasMoreElements())
            {
                String name = (String) names.nextElement();
                String value = properties.getProperty(name);
                namesAndValues.add(new PropertyNameValuePair(name, value));
            }
        }
        return namesAndValues;
    }

    private static class PropertyNameValuePair
    {
        private String name;
        private String value;

        public PropertyNameValuePair(String name, String value)
        {
            setName(name);
            setValue(value);
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }

}

