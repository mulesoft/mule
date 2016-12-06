/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.domain.type.oracle;

import org.mule.module.db.internal.domain.type.AbstractStructuredDbType;
import org.mule.util.ClassUtils;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a structured data type for the {@link #ORACLE_XMLTYPE_CLASS} class
 */
public class OracleXmlType extends AbstractStructuredDbType
{
    private static final int OPAQUE_TYPE_ID = 2007;
    private static final String XML_TYPE_NAME = "XMLTYPE";

    public static final String ORACLE_XMLTYPE_CLASS = "oracle.xdb.XMLType";

    public OracleXmlType()
    {
        super(OPAQUE_TYPE_ID, XML_TYPE_NAME);
    }

    @Override
    public Object getParameterValue(CallableStatement statement, int index) throws SQLException
    {
        return statement.getSQLXML(index);
    }

    @Override
    public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException
    {
        try
        {
            if (value instanceof String)
            {
                statement.setObject(index, createXmlType(statement.getConnection(), (String) value), OPAQUE_TYPE_ID);
                return;
            }
            if (value instanceof InputStream)
            {
                statement.setObject(index, createXmlType(statement.getConnection(), (InputStream) value), OPAQUE_TYPE_ID);
                return;
            }
        }
        catch (Exception e)
        {
            throw new SQLException(e);
        }
        super.setParameterValue(statement, index, value);
    }

    /**
     * Creates an {@link #ORACLE_XMLTYPE_CLASS} object from the received XML string
     *
     * @param connection An active database connection, required by the {@link #ORACLE_XMLTYPE_CLASS} constructor
     * @param xml A String object containing the XML content
     * @return an new {@link #ORACLE_XMLTYPE_CLASS} with the XML passed
     * @throws java.lang.Exception if there is a problem while creating the {@link #ORACLE_XMLTYPE_CLASS} object
     *   ({@link #ORACLE_XMLTYPE_CLASS} class is not found in the classpath, connection is closed, etc)
     */
    public static Object createXmlType(Connection connection, String xml) throws Exception
    {
        return createXmlType(connection, xml, String.class);
    }

    /**
     * Creates an {@link #ORACLE_XMLTYPE_CLASS} object from the received XML InputStream
     *
     * @param connection An active database connection, required by the {@link #ORACLE_XMLTYPE_CLASS} constructor
     * @param xml A Stream object containing the XML content
     * @return an new {@link #ORACLE_XMLTYPE_CLASS} with the XML passed
     * @throws java.lang.Exception if there is a problem while creating the {@link #ORACLE_XMLTYPE_CLASS} object
     *   ({@link #ORACLE_XMLTYPE_CLASS} class is not found in the classpath, connection is closed, etc)
     */
    public static Object createXmlType(Connection connection, InputStream xml) throws Exception
    {
        return createXmlType(connection, xml, InputStream.class);
    }

    private static <T> Object createXmlType(Connection connection, T xmlContent, Class<T> tClass) throws Exception
    {
        Class<?> xmlTypeClass = getXmlTypeClass();
        Constructor<?> xmlTypeConstructor = xmlTypeClass.getConstructor(Connection.class, tClass);

        return xmlTypeConstructor.newInstance(connection, xmlContent);
    }

    /**
     * Looks for the {@link #ORACLE_XMLTYPE_CLASS} class in the classpath and returns a reference to it
     *
     * @return the {@link #ORACLE_XMLTYPE_CLASS} class object
     * @throws java.lang.ClassNotFoundException if there required class in not in the classpath
     */
    public static Class<?> getXmlTypeClass() throws ClassNotFoundException
    {
        return ClassUtils.getClass(ORACLE_XMLTYPE_CLASS);
    }
}
