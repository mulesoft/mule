/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.pattern.core.config;

import org.mule.config.spring.handlers.MuleNamespaceHandler;

import org.apache.log4j.Level;
import org.junit.Test;

public class PatternDeprecationTestCase extends AbstractDeprecationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "patterns-config.xml";
    }

    @Test
    public void ensurePatternsDeprecation()
    {
        TestAppender.ensure(new TestAppender.Expectation(Level.WARN.toString(), CorePatternNamespaceHandler.class.getName(), MuleNamespaceHandler.PATTERNS_DEPRECATION_MESSAGE));
    }
}
