/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.config.domain.database;

import static junit.framework.Assert.assertEquals;

import org.mule.tck.size.SmallTest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MysqlConfigFactoryBeanTestCase
{

    private static final String URL = "jdbc:mysql://localhost:3036/database";
    private static final String URL_WITHOUT_PORT = "jdbc:mysql://localhost/database";
    private static final String URL_PROPERTIES = "?user=root&password=pass";
    private static final String METADATA_PROPERTY = "generateSimpleParameterMetadata=true";

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
        factory.setDatabase("database");
        factory.setHost("localhost");
        factory.setPort(3036);

        assertEquals(withMetadata(URL), factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithoutPropertiesAndPort()
    {
        factory.setDatabase("database");
        factory.setHost("localhost");

        assertEquals(withMetadata(URL_WITHOUT_PORT), factory.getEffectiveUrl());
    }

    @Test
    public void staticUrlWithProperties()
    {
        factory.setUrl(URL);
        properties.put("user", "root");
        properties.put("password", "pass");

        assertEquals(URL + URL_PROPERTIES + "&" + METADATA_PROPERTY, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithProperties()
    {
        factory.setDatabase("database");
        factory.setHost("localhost");
        factory.setPort(3036);

        properties.put("user", "root");
        properties.put("password", "pass");

        assertEquals(URL + URL_PROPERTIES + "&" + METADATA_PROPERTY, factory.getEffectiveUrl());
    }

    @Test
    public void dynamicUrlWithPropertiesWithoutPort()
    {
        factory.setDatabase("database");
        factory.setHost("localhost");

        properties.put("user", "root");
        properties.put("password", "pass");

        assertEquals(URL_WITHOUT_PORT + URL_PROPERTIES + "&" + METADATA_PROPERTY, factory.getEffectiveUrl());
    }

    private String withMetadata(String url)
    {
        return url + "?" + METADATA_PROPERTY;
    }

}
