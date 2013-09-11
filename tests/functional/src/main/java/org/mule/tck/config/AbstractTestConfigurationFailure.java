/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.config;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * A base test class, that starts a muleContext with the given configuration,
 * to check for an invalid argument or configuration error that cannot be
 * detected with an xml validator.<p/>
 * Parameters are set with the @Parameters annotation, as an array with a
 * {mule-config.xml, testName} tuple. In the test, muleContext.start() has to
 * be called and then check for your expected failure.
 */

@RunWith(Parameterized.class)
public abstract class AbstractTestConfigurationFailure extends AbstractMuleContextTestCase
{

    protected String configResource;
    protected String testName;

    public AbstractTestConfigurationFailure(String confResources, String name)
    {
        configResource = confResources;
        testName = name;
    }

    protected String getConfigResources()
    {
        return configResource;
    }

    @Override
    protected String getTestHeader()
    {
        return "Testing: " + name.getMethodName() + " (" + testName + ")";
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(getConfigResources());
    }
}
