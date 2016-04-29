/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * Test configuration of security filters
 */
public class SecurityFilterNonBlockingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/config/security-filter-config-nb.xml";
    }

    @Test
    public void securityFilterShouldAllowNonBlocking() throws Exception
    {
        flowRunner("nonBlockingSecurity").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

}
