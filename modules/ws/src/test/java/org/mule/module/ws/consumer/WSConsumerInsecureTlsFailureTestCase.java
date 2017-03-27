/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.mule.api.config.ConfigurationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import static org.mule.tck.util.TestUtils.loadConfiguration;

/**
 * This tests "mocks" an HTTPS server through which a wsdl file is served. The initialization of the ws consumer involved
 * in the test app should be routed through a URLConnection and should fail because of host and certificate verification.
 */
public class WSConsumerInsecureTlsFailureTestCase extends AbstractMuleContextTestCase
{
    @ClassRule
    public static DynamicPort port = new DynamicPort("port");

    @Rule
    public ExternalResource myServer = new AbstractWSDLServerTlsTestCase.ServerResource(port);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void failsToLoadWSDL() throws Exception
    {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("No name matching localhost found");

        loadConfiguration("ws-consumer-wsdl-insecure-tls-failure.xml");
    }

}
