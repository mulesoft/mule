/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty.functional;

import org.junit.Test;

/**
 * Functional tests that underlying Jetty acceptor threads may be changed in Mule Jetty HTTPS (TLS) connector
 */
public class JettyHttpsAcceptorFunctionalTestCase extends AbstractJettyAcceptorFunctionalTestCase {

    @Override
    protected String getConfigResources() {
        return "jetty-https-acceptors-functional-test.xml";
    }

    @Test
    public void testDefaultAcceptors() throws Exception {
        assertAcceptors("connector-default-acceptors", "flow-default-acceptors", 1);
    }

    @Test
    public void testAdditionalAcceptors() throws Exception {
        assertAcceptors("connector-ten-acceptors", "flow-ten-acceptors", 10);
    }
}
