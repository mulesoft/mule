/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class DiscoverProvidersTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");
    private String url;

    @Override
    protected String getConfigFile()
    {
        return "fruit-resource-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        url = "http://localhost:" + port.getNumber();
    }

    @Test
    public void cleanApple() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(url + "/tasteApple", getTestEvent("false:true").getMessage(), newOptions().method(POST.name()).disableStatusCodeValidation().build());
        assertThat(response.getPayloadAsString(), equalTo("The apple is not bitten but clean"));
    }

    @Test
    public void dirtyApple() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send(url + "/tasteApple", getTestEvent("true:false").getMessage(), newOptions().method(POST.name()).disableStatusCodeValidation().build());
        assertThat(response.getPayloadAsString(), equalTo("The apple is bitten but dirty"));
    }
}
