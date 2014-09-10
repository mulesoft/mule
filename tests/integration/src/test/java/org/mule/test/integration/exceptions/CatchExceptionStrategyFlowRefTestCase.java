/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.integration.exceptions.CatchExceptionStrategyTestCase.JSON_REQUEST;
import static org.mule.test.integration.exceptions.CatchExceptionStrategyTestCase.JSON_RESPONSE;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transaction.TransactionCoordination;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class CatchExceptionStrategyFlowRefTestCase extends FunctionalTestCase
{
    public static final int TIMEOUT = 5000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/catch-exception-strategy-flow-ref.xml";
    }

    @Test
    public void testFlowRefHandlingException() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://inExceptionBlock", JSON_REQUEST, null, TIMEOUT);
        // compare the structure and values but not the attributes' order
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualJsonNode = mapper.readTree(response.getPayloadAsString());
        JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
        assertThat(actualJsonNode, is(expectedJsonNode));
    }

    @Test
    public void testFlowRefHandlingExceptionWithTransaction() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://inTxWithException", JSON_REQUEST, null, TIMEOUT);
        // compare the structure and values but not the attributes' order
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualJsonNode = mapper.readTree(response.getPayloadAsString());
        JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
        assertThat(actualJsonNode, is(expectedJsonNode));
    }

    public static class VerifyTransactionNotResolvedProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            assertThat(tx, IsNull.<Object>notNullValue());
            assertThat(tx.isRollbackOnly(), Is.is(false));
            return event;
        }
    }
}
