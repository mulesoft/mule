/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

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
