/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.config.domain.database;

import static junit.framework.Assert.assertEquals;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MysqlConfigFactoryBeanTestCase extends AbstractMuleTestCase
{

    private static final String URL = "jdbc:mysql://localhost:3036/database";
    private static final String URL_WITHOUT_PORT = "jdbc:mysql://localhost/database";
    private static final String URL_PROPERTIES = "?user=root&password=pass";
    private static final String METADATA_PROPERTY = "generateSimpleParameterMetadata=true";
    private static final String DATABASE = "database";
    private static final String HOST = "localhost";
    private static final int PORT = 3036;
    private static final String USER_PROPERTY = "user";
    private static final String USER_VALUE = "root";
    private static final String PASSWORD_PROPERTY = "password";
    private static final String PASSWORD_VALUE = "pass";

    private MySqlConfigFactoryBean factory;
    private Map<String, String> properties;

    @Before
    public void setUp() throws Exception
    {
        factory = new MySqlConfigFactoryBean();

        properties = new LinkedHashMap<String, String>();
        factory.setProperties(properties);
    }

    @Test
    public void staticUrlWithoutProperties()
    {
        factory.setUrl(URL);
        assertEquals(withMetadata(URL), factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithoutProperties()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);
        factory.setPort(PORT);

        assertEquals(withMetadata(URL), factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithoutPropertiesAndPort()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);

        assertEquals(withMetadata(URL_WITHOUT_PORT), factory.getEffectiveUrl());
    }

    @Test
    public void staticUrlWithProperties()
    {
        factory.setUrl(URL);
        properties.put(USER_PROPERTY, USER_VALUE);
        properties.put(PASSWORD_PROPERTY, PASSWORD_VALUE);

        assertEquals(URL + URL_PROPERTIES + "&" + METADATA_PROPERTY, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithProperties()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);
        factory.setPort(3036);

        properties.put(USER_PROPERTY, USER_VALUE);
        properties.put(PASSWORD_PROPERTY, PASSWORD_VALUE);

        assertEquals(URL + URL_PROPERTIES + "&" + METADATA_PROPERTY, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithPropertiesWithoutPort()
    {
        factory.setDatabase(DATABASE);
        factory.setHost(HOST);

        properties.put(USER_PROPERTY, USER_VALUE);
        properties.put(PASSWORD_PROPERTY, PASSWORD_VALUE);

        assertEquals(URL_WITHOUT_PORT + URL_PROPERTIES + "&" + METADATA_PROPERTY, factory.getEffectiveUrl());
    }

    private String withMetadata(String url)
    {
        return url + "?" + METADATA_PROPERTY;
    }

}
