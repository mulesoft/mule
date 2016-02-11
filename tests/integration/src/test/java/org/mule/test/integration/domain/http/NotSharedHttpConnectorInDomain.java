/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class NotSharedHttpConnectorInDomain extends DomainFunctionalTestCase
{

    private static final String APP = "app";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getDomainConfig()
    {
        return "domain/empty-domain-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                new ApplicationConfig(APP, new String[] {"domain/http/http-not-shared-listener-config.xml"})
        };
    }

    @Test
    public void sendMessageToNotSharedConnectorInDomain() throws Exception {
        String url = String.format("http://localhost:%d/test", dynamicPort.getNumber());
        MuleContext muleContext = getMuleContextForApp(APP);
        muleContext.getClient().send(url, new DefaultMuleMessage("", muleContext));

        assertThat(muleContext.getClient().request("test://in", 5000), is(notNullValue()));
    }
}
