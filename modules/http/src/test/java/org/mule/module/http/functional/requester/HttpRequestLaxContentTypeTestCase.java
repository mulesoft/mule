/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestLaxContentTypeTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Rule
    public SystemProperty laxContentType = new SystemProperty(SYSTEM_PROPERTY_PREFIX + "laxContentType", Boolean.TRUE.toString());

    @Override
    protected String getConfigFile()
    {
        return "http-request-lax-content-type-config.xml";
    }

    @Test
    public void sendsInvalidContentTypeOnRequest() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        final String url = String.format("http://localhost:%s/requestClientInvalid", httpPort.getNumber());

        MuleMessage response = client.send(url, TEST_MESSAGE, null);

        assertNoContentTypeProperty(response);
        assertThat(response.getPayloadAsString(), equalTo("invalidMimeType"));
    }

    private void assertNoContentTypeProperty(MuleMessage response)
    {
        assertThat(response.getInboundPropertyNames(), not(hasItem(equalToIgnoringCase(MuleProperties.CONTENT_TYPE_PROPERTY))));
    }
}
