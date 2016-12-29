/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.internal.request.ResponseValidatorException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class HttpSocketTimeoutTestCase extends FunctionalTestCase {

    @Rule
    public DynamicPort port = new DynamicPort("port");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile() {
        return "http-socket-timeout-config.xml";
    }

    @Test
    public void usesSoTimeoutIfAvailable() throws MuleException {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send(getUrl("timeout"), getTestMuleMessage(), 25000);
        assertThat(message, notNullValue());
        assertThat(message.getPayload(), notNullValue());
    }

    @Test
    public void usesResponseTimeoutByDefault() throws MuleException {
        MuleClient client = muleContext.getClient();
        expectedException.expect(ResponseValidatorException.class);
        client.send(getUrl("noTimeout"), getTestMuleMessage(), 25000);
    }

    private String getUrl(String path) {
        return String.format("http://localhost:%s/%s", port.getValue(), path);
    }


}
