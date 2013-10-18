/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.util.ClassUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Initializes the XStream utility for converting Objects to XML and XML to Objects.
 */
// @Immutable
public class XStreamFactory
{
    public static final String XSTREAM_DOM_DRIVER = "com.thoughtworks.xstream.io.xml.DomDriver";
    public static final String XSTREAM_DOM4J_DRIVER = "com.thoughtworks.xstream.io.xml.Dom4JDriver";
    public static final String XSTREAM_JDOM_DRIVER = "com.thoughtworks.xstream.io.xml.JDomDriver";
    public static final String XSTREAM_STAX_DRIVER = "com.thoughtworks.xstream.io.xml.StaxDriver";
    public static final String XSTREAM_XPP_DRIVER = "com.thoughtworks.xstream.io.xml.XppDriver";

    private static final Log logger = LogFactory.getLog(XStreamFactory.class);

    private final XStream xstream;

    public XStreamFactory() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        this(XSTREAM_XPP_DRIVER, null, null);
    }

    public XStreamFactory(String driverClassName, Map<String, Class<?>> aliases, Set<Class <? extends Converter>> converters)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        Class<?> driverClass = ClassUtils.loadClass(driverClassName, this.getClass());
        xstream = new XStream((HierarchicalStreamDriver) driverClass.newInstance());

        // We must always register this converter as the Mule Message uses
        // ConcurrentHashMaps, but XStream currently does not support them out of the
        // box.
        xstream.registerConverter(new XStreamFactory.ConcurrentHashMapConverter(xstream.getMapper()), -1);

        registerAliases(aliases);
        registerConverters(converters);
    }

    private void registerAliases(Map<String, Class<?>> aliases)
    {
        if (aliases != null)
        {
            for (Map.Entry<String, Class<?>> entry : aliases.entrySet())
            {
                xstream.alias(entry.getKey(), entry.getValue());
            }
        }
    }

    private void registerConverters(Set<Class <? extends Converter>> converters) throws InstantiationException, IllegalAccessException
    {
        if (converters != null)
        {
            for (Class<?> converter : converters)
            {
                Object converterInstance = converter.newInstance();
                if (converterInstance instanceof Converter)
                {
                    xstream.registerConverter((Converter) converterInstance);
                }
                else if (converterInstance instanceof SingleValueConverter)
                {
                    xstream.registerConverter((SingleValueConverter) converterInstance);
                }
                else
                {
                    logger.warn("Invalid converter class specified - ignoring: " + converter.getName());
                }
            }
        }
    }

    public final XStream getInstance()
    {
        return xstream;
    }

    private class ConcurrentHashMapConverter extends MapConverter
    {
        public ConcurrentHashMapConverter(Mapper mapper) throws ClassNotFoundException
        {
            super(mapper);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean canConvert(Class aClass)
        {
            String className = aClass.getName();
            return className.equals("java.util.concurrent.ConcurrentHashMap");
        }
    }
}
