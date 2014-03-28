/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.integration.config;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DatasourceWithConnectionPropertiesBeanConfigTestCase extends AbstractMuleContextTestCase
{

    @Test(expected = ConfigurationException.class)
    public void expectFailure() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder("integration/config/bean-datasource-with-connection-properties-config.xml");
        configBuilder.configure(muleContext);
    }

}
