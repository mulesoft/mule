/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


