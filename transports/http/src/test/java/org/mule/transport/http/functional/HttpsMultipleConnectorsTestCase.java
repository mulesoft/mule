/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.api.security.tls.TlsConfiguration.DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY;
import org.mule.api.transport.DispatchException;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.net.ssl.SSLHandshakeException;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test case that verifies that multiple HTTPS connectors do not interfere with each other. The server endpoint
 * uses an HTTP connector with a custom certificate. The client endpoint has no key store configured, therefore it
 * should not be able to connect to the server. However, with the current behavior of the HTTPS transport, it will work.
 * This is because HTTPS connectors set system properties with the key store and trust store, that later on become the
 * default for any SSL connection. The system property "mule.tls.disableSystemPropertiesMapping" disables this behavior,
 * and this test verifies that the two connectors will not interfere when this property is set.
 */
public class HttpsMultipleConnectorsTestCase extends FunctionalTestCase
{
    private static final String TRUST_STORE_SYSTEM_PROPERTY = "javax.net.ssl.trustStore";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Rule
    public SystemProperty disablePropertiesMapping = new SystemProperty(DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY, "true");

    private String oldTrustStoreValue;

    @Override
    protected String getConfigFile()
    {
        return "https-multiple-connectors-config.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();

        /* The trust store might have been set by other test running in the same process, we need to clear it
         * temporarily in order to run this test correctly. */
        oldTrustStoreValue = System.clearProperty(TRUST_STORE_SYSTEM_PROPERTY);
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        super.doTearDownAfterMuleContextDispose();

        if (oldTrustStoreValue != null)
        {
            System.setProperty(TRUST_STORE_SYSTEM_PROPERTY, oldTrustStoreValue);
        }
    }

    @Test
    public void connectorWithInvalidKeyStoreFails() throws Exception
    {
        Flow client = (Flow) getFlowConstruct("client");

        try
        {
            client.process(getTestEvent(TEST_MESSAGE));
            fail();
        }
        catch (DispatchException e)
        {
            assertThat(e.getCause(), instanceOf(SSLHandshakeException.class));
        }
    }
}
