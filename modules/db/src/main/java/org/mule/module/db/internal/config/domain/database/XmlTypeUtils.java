/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import org.mule.util.ClassUtils;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.sql.Connection;

/**
 * Provides utility methods to work with oracle.xdb.XMLType values
 */
public class XmlTypeUtils
{

    public static final String ORACLE_XMLTYPE_CLASS = "oracle.xdb.XMLType";

    private XmlTypeUtils()
    {
    }

    /**
     * Creates an oracle XMLType object from the received xml string
     *
     * @param connection An active oracle database connection, required by the XmlType constructor
     * @param xml A String object containing the xml content
     * @return an new oracle XmlType with the xml passed
     * @throws java.lang.Exception if there is a problem while creating the XmlType object
     *   (XmlType class is not found in the classpath, connection is closed, etc)
     */
    public static Object createXmlType(Connection connection, String xml) throws Exception
    {
        return createXmlType(connection, xml, String.class);
    }

    /**
     * Creates an oracle XMLType object from the received xml InputStream
     *
     * @param connection An active oracle database connection, required by the XmlType constructor
     * @param xml A Stream object containing the xml content
     * @return an new oracle XmlType with the xml passed
     * @throws java.lang.Exception if there is a problem while creating the XmlType object
     *   (XmlType class is not found in the classpath, connection is closed, etc)
     */
    public static Object createXmlType(Connection connection, InputStream xml) throws Exception
    {
        return createXmlType(connection, xml, InputStream.class);
    }

    private static <T> Object createXmlType(Connection connection, T xml, Class<T> tClass) throws Exception
    {
        Class<?> xmlTypeClass = getXmlTypeClass();
        Constructor<?> xmlTypeConstructor = xmlTypeClass.getConstructor(Connection.class, tClass);

        return xmlTypeConstructor.newInstance(connection, xml);
    }

    /**
     * Looks for the oracle XmlType class in the classpath and returns a reference to it
     *
     * @return the oracle XmlType class object
     * @throws java.lang.ClassNotFoundException if there required class in no in the classpath
     */
    public static Class<?> getXmlTypeClass() throws ClassNotFoundException
    {
        return ClassUtils.getClass(ORACLE_XMLTYPE_CLASS);
    }
}
