/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import static org.junit.Assert.fail;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

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
