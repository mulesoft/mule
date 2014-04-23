/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.endpoints;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractEndpointEncodedUrlTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {"org/mule/test/integration/endpoints/secure-test-server-config.xml", getEncodedUrlConfigFile()};
    }

    protected abstract String getEncodedUrlConfigFile();

    @Test
    public void resolvesDynamicAddress() throws Exception
    {
        doEncodedUrlTest(getDynamicUrl());
    }

    @Test
    public void resolvesAssembledDynamicAddress() throws Exception
    {
        doEncodedUrlTest(getAssembledDynamicUrl());
    }

    @Test
    public void resolvesStaticAddress() throws Exception
    {
        doEncodedUrlTest(getStaticUrl());
    }

    @Test
    public void resolvesAssembledStaticAddress() throws Exception
    {
        doEncodedUrlTest(getAssembledStaticUrl());
    }

    protected void doEncodedUrlTest(String inputUrl) throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put("path", "test");
        MuleMessage response = client.send(inputUrl, TEST_MESSAGE, messageProperties);

        assertThat(response.getPayloadAsString(), equalTo("Processed: bar"));
    }

    protected abstract String getDynamicUrl();

    protected abstract String getAssembledDynamicUrl();

    protected abstract String getStaticUrl();

    protected abstract String getAssembledStaticUrl();
}
