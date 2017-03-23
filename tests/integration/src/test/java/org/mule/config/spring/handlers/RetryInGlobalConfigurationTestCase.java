/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.retry.notifiers.ConnectNotifier;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class RetryInGlobalConfigurationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/handlers/retry-in-global-config-config.xml";
    }

    @Test
    public void testGlobalRetryReconnectConfiguration() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupConnector("testConnector");
        assertThat(c, notNullValue());

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertThat(rpf, notNullValue());
        RetryNotifier rn = rpf.getNotifier();
        assertThat(rn, notNullValue());
        assertThat(rn, instanceOf(ConnectNotifier.class));

        assertThat(c.isConnected(), equalTo(true));
        assertThat(c.isStarted(), equalTo(true));
    }
}
