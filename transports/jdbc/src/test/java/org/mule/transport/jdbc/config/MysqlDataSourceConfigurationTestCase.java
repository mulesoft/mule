/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;

import org.junit.Test;

public class MysqlDataSourceConfigurationTestCase extends AbstractDataSourceConfigurationTestCase
{
    @Test(expected=ConfigurationException.class)
    public void failWhenUrlAndDatabaseConfigured() throws MuleException
    {
        tryBuildingMuleContextFromInvalidConfig("jdbc-data-source-mysql-url-and-database.xml");
    }

    @Test(expected=ConfigurationException.class)
    public void failWhenUrlAndHostConfigured() throws MuleException
    {
        tryBuildingMuleContextFromInvalidConfig("jdbc-data-source-mysql-url-and-host.xml");
    }
}
