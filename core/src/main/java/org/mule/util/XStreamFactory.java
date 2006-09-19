/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Initializes the XStream utility for converting Objects to XML and XML to Objects.
 */
public class XStreamFactory
{
    private static XStream xstream;

    public XStreamFactory() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
    	this(false, null, null);
    }

    public XStreamFactory(boolean useJaxpDom, Map aliases, List converters) throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        if (useJaxpDom) {
            xstream = new XStream(new DomDriver());
        } else {
            xstream = new XStream(new XppDriver());
        }

        // We must always register this converter as the Mule Message uses
        // ConcurrentHashMaps, but XStream does not support them out of the box right now
        xstream.registerConverter(new ConcurrentHashMapConverter(xstream.getClassMapper()), -1);

        addAliases(aliases);
        addConverters(converters);
    }

    public final XStream getInstance()
    {
        return xstream;
    }

    private void addAliases(Map aliases) throws ClassNotFoundException
    {
        if (aliases == null) {
            return;
        }

        Map.Entry entry;
        String classname;
        Class clazz;
        for (Iterator iterator = aliases.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            classname = entry.getValue().toString();
            clazz = ClassUtils.loadClass(classname, getClass());
            xstream.alias(entry.getKey().toString(), clazz);
        }
    }

    private void addConverters(List converters) throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        if (converters == null) {
            return;
        }
        String classname;
        Class clazz;

        for (Iterator iterator = converters.iterator(); iterator.hasNext();) {
            classname = iterator.next().toString();
            clazz = ClassUtils.loadClass(classname, getClass());
            xstream.registerConverter((Converter) clazz.newInstance());
        }
    }

    private class ConcurrentHashMapConverter extends MapConverter
    {
        private Class jdk5ConurrentHashMap = null;
        private Class backportConurrentHashMap = null;

        public ConcurrentHashMapConverter(Mapper mapper) throws ClassNotFoundException {
            super(mapper);
            try {
                jdk5ConurrentHashMap = ClassUtils.loadClass("java.util.concurrent.ConcurrentHashMap", getClass());
            } catch (ClassNotFoundException e) {
                // ignore: probably running on JDK 1.4
            }

            try {
                backportConurrentHashMap = ClassUtils.loadClass("edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap", getClass());
            } catch (ClassNotFoundException e) {
                // ignore: maybe running Mule 3.0 with native util.concurrent :)
            }

            // ..however if both are null something is wrong.
            if (jdk5ConurrentHashMap == null && backportConurrentHashMap == null) {
                throw new ClassNotFoundException("Neither java.util.concurrent.ConcurrentHashMap nor edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap could be found - cannot continue.");
            }
        }

        public boolean canConvert(Class aClass) {
            return aClass.equals(backportConurrentHashMap) || aClass.equals(jdk5ConurrentHashMap);
        }
    }
}
