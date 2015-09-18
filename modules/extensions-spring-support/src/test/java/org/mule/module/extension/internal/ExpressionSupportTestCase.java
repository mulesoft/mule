/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.tck.junit4.ExtensionFunctionalTestCase;

import org.junit.Test;

public class ExpressionSupportTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }

    @Test(expected = ConfigurationException.class)
    public void expressionRequiredButFixedValueInstead() throws Exception
    {
        tryConfigure("heisenberg-invalid-expression-parameter.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void fixedValueRequiredButExpressionInstead() throws Exception
    {
        tryConfigure("heisenberg-fixed-parameter-with-expression.xml.xml");
    }

    private void tryConfigure(String configResource) throws Exception
    {
        MuleContext context = new DefaultMuleContextFactory().createMuleContext();
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(configResource);
        builder.configure(context);
    }
}
