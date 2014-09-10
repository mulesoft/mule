/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class ExceptionStrategyFlowRefTestCase extends FunctionalTestCase
{
    public static final String MESSAGE = "some message";
    public static final int TIMEOUT = 5000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-flow-ref.xml";
    }

    @Test
    public void testExceptionInFlowCalledWithFlowRef() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://inExceptionBlock", MESSAGE, null, TIMEOUT);
        MuleMessage response = client.request("jms://dlq", TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.<String>getInboundProperty("mainEs"), Is.is("yes"));
        assertThat(response.<String>getInboundProperty("flowRefEs"), Is.is("yes"));
    }
}
