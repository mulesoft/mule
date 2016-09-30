/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.xml_security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.OperationOptions;
import org.mule.module.http.api.HttpConstants.ResponseProperties;

import org.junit.Test;

public class XmlBlDisabledTestCase extends XmlBlBase
{
    @Test
    public void doesNotExpandEntitiesWhenDisabled() throws Exception
    {
        MuleMessage testMuleMessage = getTestMuleMessage(TEST_MESSAGE);
        testMuleMessage.setPayload(xmlWithEntities);
        testMuleMessage.setProperty("Content-Type", "application/xml");

        OperationOptions options = newOptions().method("POST").disableStatusCodeValidation().build();
        MuleMessage result = client.send(url, testMuleMessage, options);

        int status = result.getInboundProperty(ResponseProperties.HTTP_STATUS_PROPERTY);
        assertThat(status, not(is(OK.getStatusCode())));
        assertThat(result.getPayloadAsString(), not(containsString("0101")));
    }
}


