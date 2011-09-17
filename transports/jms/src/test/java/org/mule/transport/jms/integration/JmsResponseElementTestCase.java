/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

    public class JmsResponseElementTestCase extends AbstractJmsFunctionalTestCase {

    public static final String MESSAGE = "A Message";
    public static final String EXPECTED_MODIFIED_MESSAGE = "A Message jms flow content";

    @Override
    protected String getConfigResources() {
        return "integration/jms-response-element.xml";
    }

    @Test
    public void testOutboundEndopintResponse() throws Exception {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://vminbound", "some message", null);
        assertThat(response.getPayloadAsString(), is(EXPECTED_MODIFIED_MESSAGE));
        assertThat(response.<String>getProperty("test", PropertyScope.INBOUND),Is.is("test"));
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }

}
