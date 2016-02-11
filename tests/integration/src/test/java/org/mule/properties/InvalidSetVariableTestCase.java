/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InvalidSetVariableTestCase extends AbstractMuleTestCase
{
    private String muleConfigPath;

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                                             {"org/mule/properties/invalid-set-property.xml"},
                                             {"org/mule/properties/invalid-set-variable.xml"}
        });
    }

    public InvalidSetVariableTestCase(String muleConfigPath)
    {
        this.muleConfigPath = muleConfigPath;
    }

    @Test(expected = ConfigurationException.class)
    public void emptyVariableNameValidatedBySchema() throws Exception
    {
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(muleConfigPath);
        builder.configure(context);
    }
}
