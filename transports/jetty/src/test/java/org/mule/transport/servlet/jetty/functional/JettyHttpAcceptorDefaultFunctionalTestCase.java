/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import org.junit.Test;

/**
 * Functional tests that underlying Jetty acceptor threads may be changed in Mule Jetty HTTP connector.
 * Verifies the default number of acceptor threads.
 */
public class JettyHttpAcceptorDefaultFunctionalTestCase extends AbstractJettyAcceptorFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jetty-http-acceptors-default-functional-test.xml";
    }

    @Test
    public void testDefaultAcceptors() throws Exception
    {
        assertAcceptors("connector-default-acceptors", "flow-default-acceptors", 1, Protocol.http);
    }
}
