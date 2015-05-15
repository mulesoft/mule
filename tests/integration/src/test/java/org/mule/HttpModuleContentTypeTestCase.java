/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class HttpModuleContentTypeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-module-content-type-config.xml";
    }

    @Test
    public void returnsContentTypeInResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("http://localhost:8888/api", TEST_MESSAGE, null);

        assertThat(response.getInboundPropertyNames(), hasItem(equalToIgnoringCase(MuleProperties.CONTENT_TYPE_PROPERTY)));
        assertThat((String) response.getInboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY), equalTo("application/json; charset=UTF-8"));
    }

    @Test
    public void sendContentTypeOnRequest() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("http://localhost:8888/requestClient", TEST_MESSAGE, null);

        assertThat(response.getInboundPropertyNames(), not(hasItem(equalToIgnoringCase(MuleProperties.CONTENT_TYPE_PROPERTY))));
        assertThat(response.getPayloadAsString(), equalTo("application/json; charset=UTF-8"));
    }
}
