/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.pattern.core.config;

import static org.apache.logging.log4j.Level.WARN;
import static org.mule.config.spring.handlers.MuleNamespaceHandler.PATTERNS_DEPRECATION_MESSAGE;

import org.mule.config.spring.parsers.specific.BridgeDefinitionParser;
import org.mule.tck.logging.TestAppender;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Test;

public class CoreDeprecationTestCase extends AbstractDeprecationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "core-config.xml";
    }

    @Test
    public void ensureCoreDeprecation()
    {
        new PollingProber(200, 50).check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                testAppender.ensure(new TestAppender.Expectation(WARN.toString(), BridgeDefinitionParser.class.getName(), PATTERNS_DEPRECATION_MESSAGE));
                return true;
            }
        });
    }
}
