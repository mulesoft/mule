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

import org.junit.Test;

public class InvalidSetVariableTestCase extends AbstractMuleTestCase
{
    @Test(expected = ConfigurationException.class)
    public void emptyVariableName() throws Exception
    {
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder("org/mule/properties/invalid-set-variable.xml");
        builder.configure(context);
    }
}
