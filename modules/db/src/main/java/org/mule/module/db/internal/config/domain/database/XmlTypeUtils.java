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

    public static Object createXmlType(Connection connection, String xml) throws Exception
    {
        return genericCreateXmlType(connection, xml, String.class);
    }

    public static Object createXmlType(Connection connection, InputStream xml) throws Exception
    {
        return genericCreateXmlType(connection, xml, InputStream.class);
    }

    private static <T> Object genericCreateXmlType(Connection connection, T xml, Class<T> tClass) throws Exception
    {
        Class<?> xmlTypeClass = getXmlTypeClass();
        Constructor<?> xmlTypeConstructor = xmlTypeClass.getConstructor(Connection.class, tClass);

        return xmlTypeConstructor.newInstance(connection, xml);
    }

    public static Class<?> getXmlTypeClass() throws ClassNotFoundException
    {
        return ClassUtils.getClass(ORACLE_XMLTYPE_CLASS);
    }
}
