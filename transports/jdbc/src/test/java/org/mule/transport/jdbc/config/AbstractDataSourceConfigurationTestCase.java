/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.config;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import static org.junit.Assert.fail;

public class AbstractDataSourceConfigurationTestCase extends AbstractMuleTestCase
{
    protected void tryBuildingMuleContextFromInvalidConfig(String config) throws MuleException
    {
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        ConfigurationBuilder builder = new SpringXmlConfigurationBuilder(config);
        muleContextFactory.createMuleContext(builder);
        fail();
    }
}
