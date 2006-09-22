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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Initializes the XStream utility for converting Objects to XML and XML to Objects.
 */
public class XStreamFactory
{
    public static final String XSTREAM_DOM_DRIVER = "com.thoughtworks.xstream.io.xml.DomDriver";
    public static final String XSTREAM_DOM4J_DRIVER = "com.thoughtworks.xstream.io.xml.Dom4JDriver";
    public static final String XSTREAM_STAX_DRIVER = "com.thoughtworks.xstream.io.xml.StaxDriver";
    public static final String XSTREAM_XPP_DRIVER = "com.thoughtworks.xstream.io.xml.XppDriver";

    private final XStream xstream;

    public XStreamFactory() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        this(XSTREAM_XPP_DRIVER, null, null);
    }

    public XStreamFactory(String driverClassName, Map aliases, List converters)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        Class driverClass = ClassUtils.loadClass(driverClassName, this.getClass());
        xstream = new XStream((HierarchicalStreamDriver)driverClass.newInstance());

        // We must always register this converter as the Mule Message uses
        // ConcurrentHashMaps, but XStream currently does not support them out of the
        // box
        xstream.registerConverter(new ConcurrentHashMapConverter(xstream.getClassMapper()), -1);

        if (aliases != null)
        {
            for (Iterator iterator = aliases.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry)iterator.next();
                Class aliasClass = ClassUtils.loadClass(entry.getValue().toString(), getClass());
                xstream.alias(entry.getKey().toString(), aliasClass);
            }
        }

        if (converters != null)
        {
            for (Iterator iterator = converters.iterator(); iterator.hasNext();)
            {
                Class converterClazz = ClassUtils.loadClass(iterator.next().toString(), getClass());
                xstream.registerConverter((Converter)converterClazz.newInstance());
            }
        }
    }

    public final XStream getInstance()
    {
        return xstream;
    }

    private class ConcurrentHashMapConverter extends MapConverter
    {
        private Class jdk5ConcurrentHashMap = null;
        private Class backportConcurrentHashMap = null;

        public ConcurrentHashMapConverter(Mapper mapper) throws ClassNotFoundException
        {
            super(mapper);
            try
            {
                jdk5ConcurrentHashMap = ClassUtils.loadClass("java.util.concurrent.ConcurrentHashMap",
                    getClass());
            }
            catch (ClassNotFoundException e)
            {
                // ignore: probably running on JDK 1.4
            }

            try
            {
                backportConcurrentHashMap = ClassUtils.loadClass(
                    "edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap", getClass());
            }
            catch (ClassNotFoundException e)
            {
                // ignore: maybe running Mule 3.0 with native util.concurrent :)
            }

            // ..however if both are null something is wrong.
            if (jdk5ConcurrentHashMap == null && backportConcurrentHashMap == null)
            {
                throw new ClassNotFoundException(
                    "Neither java.util.concurrent.ConcurrentHashMap nor edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap could be found - cannot continue.");
            }
        }

        public boolean canConvert(Class aClass)
        {
            return aClass.equals(backportConcurrentHashMap) || aClass.equals(jdk5ConcurrentHashMap);
        }
    }
}
