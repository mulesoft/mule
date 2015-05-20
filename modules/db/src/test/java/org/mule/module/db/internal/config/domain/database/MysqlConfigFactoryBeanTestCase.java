/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.config.domain.database;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MysqlConfigFactoryBeanTestCase extends AbstractMuleTestCase
{

    private static final String URL = "jdbc:mysql://localhost:3036/database";
    private static final String URL_WITHOUT_PORT = "jdbc:mysql://localhost/database";
    private static final String URL_PROPERTIES = "?user=root&password=pass";
    private static final String DATABASE = "database";
    private static final String HOST = "localhost";
    private static final int PORT = 3036;
    private static final String USER_PROPERTY = "user";
    private static final String USER_VALUE = "root";
    private static final String PASSWORD_PROPERTY = "password";
    private static final String PASSWORD_VALUE = "pass";

    private MySqlConfigResolverFactoryBean factory;
    private Map<String, String> properties;

    @Before
    public void setUp() throws Exception
    {
        factory = new MySqlConfigResolverFactoryBean();

        properties = new LinkedHashMap<String, String>();
        factory.setConnectionProperties(properties);
    }

    @Test
    public void staticUrlWithoutProperties()
    {
        factory.setUrl(URL);
        assertEquals(URL, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithoutProperties()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);
        factory.setPort(PORT);

        assertEquals(URL, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithoutPropertiesAndPort()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);

        assertEquals(URL_WITHOUT_PORT, factory.getEffectiveUrl());
    }

    @Test
    public void staticUrlWithProperties()
    {
        factory.setUrl(URL);
        properties.put(USER_PROPERTY, USER_VALUE);
        properties.put(PASSWORD_PROPERTY, PASSWORD_VALUE);

        assertEquals(URL + URL_PROPERTIES, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithProperties()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);
        factory.setPort(PORT);

        properties.put(USER_PROPERTY, USER_VALUE);
        properties.put(PASSWORD_PROPERTY, PASSWORD_VALUE);

        assertEquals(URL + URL_PROPERTIES, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithPropertiesWithoutPort()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);

        properties.put(USER_PROPERTY, USER_VALUE);
        properties.put(PASSWORD_PROPERTY, PASSWORD_VALUE);

        assertEquals(URL_WITHOUT_PORT + URL_PROPERTIES, factory.getEffectiveUrl());
    }

    @Test
    public void validateNoDatasourceNorProperties() throws Exception
    {
        factory.validate();
    }

    @Test
    public void validateDatasourceWithoutProperties() throws Exception
    {
        factory.setDataSource(mock(DataSource.class));
        factory.validate();
    }

    @Test(expected = IllegalStateException.class)
    public void validateDatasourceWithProperties() throws Exception
    {
        factory.setDataSource(mock(DataSource.class));
        properties.put(USER_PROPERTY, USER_VALUE);

        factory.validate();
    }
}
