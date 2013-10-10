/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.config;

import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class ScriptingConfigErrorTestCase extends AbstractMuleTestCase
{

    @Test(expected = ConfigurationException.class)
    public void testMissingEngine() throws InitialisationException, ConfigurationException
    {
        new DefaultMuleContextFactory().createMuleContext("config-error.xml");
    }
}


