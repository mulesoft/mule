/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class NonBlockingPartlySupportedFunctionalTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "non-blocking-partly-supported-test-config.xml";
    }

    @Test
    public void foreach() throws Exception
    {
        flowRunner("foreach").withPayload(TEST_MESSAGE).nonBlocking().run();

    }

    @Test
    public void wiretap() throws Exception
    {
        flowRunner("wiretap").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

}

