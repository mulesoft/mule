/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * This tests that we can have references to management context aware objects within a config
 */
public class MuleContextAwareTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "management-context-aware-test-flow.xml";
    }

    @Test
    public void testStartup()
    {
        // only want startup to succeed
    }

}
