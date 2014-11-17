/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class HttpRequestWithMuleClientTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-request-mule-client-config.xml";
    }

    @Test
    public void muleClientUsingHttpConnector() throws Exception
    {
        muleContext.getClient().dispatch(getUrl(), TEST_MESSAGE, new HashMap<String, Object>());
        final MuleMessage vmMessage = muleContext.getClient().request("vm://out", RECEIVE_TIMEOUT);
        assertThat(vmMessage, notNullValue());
        assertThat(vmMessage.getPayloadAsString(), is(TEST_MESSAGE));
    }

    private String getUrl()
    {
        return String.format("http://localhost:%s/path", port.getNumber());
    }
}
