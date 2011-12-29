/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.integration.exceptions.HandleExceptionStrategyTestCase.JSON_REQUEST;
import static org.mule.test.integration.exceptions.HandleExceptionStrategyTestCase.JSON_RESPONSE;

public class HandleExceptionStrategyFlowRefTestCase extends FunctionalTestCase
{

    public static final int TIMEOUT = 5000;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/handle-exception-strategy-flow-ref.xml";
    }

    @Test
    public void testFlowRefHandlingException() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://inExceptionBlock", JSON_REQUEST, null, TIMEOUT);
        assertThat(response.getPayloadAsString(), is(JSON_RESPONSE));
    }
}
