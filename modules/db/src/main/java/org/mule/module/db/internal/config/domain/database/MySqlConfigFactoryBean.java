/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import java.util.Map;

public class MySqlConfigFactoryBean extends AbstractVendorConfigFactoryBean
{

    private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
    private static final String MYSQL_URL_PREFIX = "jdbc:mysql://";

    public MySqlConfigFactoryBean()
    {
        super(MYSQL_URL_PREFIX);

        setDriverClassName(DRIVER_CLASS_NAME);
    }

    @Override
    public Map<String, String> getConnectionProperties()
    {
        Map<String, String> connectionProperties = super.getConnectionProperties();

        if (MySqlConfigFactoryBean.DRIVER_CLASS_NAME.equals(getDriverClassName()))
        {
            connectionProperties.put("generateSimpleParameterMetadata", "true");
        }

        return connectionProperties;
    }
}