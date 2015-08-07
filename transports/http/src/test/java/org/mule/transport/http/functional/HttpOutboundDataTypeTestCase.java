/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.types.MimeTypes;

import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

public class HttpOutboundDataTypeTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "http-datatype-config.xml";
    }

    @Test
    public void propagatesDataType() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        muleMessage.setOutboundProperty("Content-Type", MimeTypes.TEXT + "; charset=" + StandardCharsets.UTF_16.name());

        client.dispatch("vm://testInput", muleMessage);

        MuleMessage response = client.request("vm://testOutput", 120000);

        assertThat(response.getDataType().getMimeType(), equalTo(MimeTypes.TEXT));
        assertThat(response.getDataType().getEncoding(), equalTo(StandardCharsets.UTF_16.name()));
        assertThat(response.getOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY), is(nullValue()));
        assertThat(response.getOutboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY), is(nullValue()));
    }
}
