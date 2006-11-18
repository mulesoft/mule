/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import org.mule.util.ClassUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        // box.
        // TODO see MULE-1151 on how to upgrade this code to be XStream-1.2
        // compliant, maybe for Mule 1.4/2.0.
        xstream.registerConverter(new XStreamFactory.ConcurrentHashMapConverter(xstream.getClassMapper()), -1);

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
        public ConcurrentHashMapConverter(Mapper mapper) throws ClassNotFoundException
        {
            super(mapper);
        }

        public boolean canConvert(Class aClass)
        {
            String className = aClass.getName();
            return className.equals("java.util.concurrent.ConcurrentHashMap")
                   || className.equals("edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap");
        }
    }

}
