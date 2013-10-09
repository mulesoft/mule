/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.config;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;

import org.junit.Test;

public class DerbyDataSourceConfigurationTestCase extends AbstractDataSourceConfigurationTestCase
{
    @Test(expected=ConfigurationException.class)
    public void failWhenUrlAndDatabaseConfigured() throws MuleException
    {
        tryBuildingMuleContextFromInvalidConfig("jdbc-data-source-derby-url-and-database.xml");
    }
}
