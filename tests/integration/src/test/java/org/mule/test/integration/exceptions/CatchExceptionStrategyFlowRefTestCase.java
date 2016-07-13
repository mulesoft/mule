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
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.transaction.TransactionCoordination;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class CatchExceptionStrategyFlowRefTestCase extends AbstractIntegrationTestCase
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
        MuleMessage response = flowRunner("exceptionHandlingBlock").withPayload(JSON_REQUEST).run().getMessage();
        // compare the structure and values but not the attributes' order
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualJsonNode = mapper.readTree(getPayloadAsString(response));
        JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
        assertThat(actualJsonNode, is(expectedJsonNode));
    }

    @Test
    public void testFlowRefHandlingExceptionWithTransaction() throws Exception
    {
        MuleMessage response = flowRunner("transactionNotResolvedAfterException").withPayload(JSON_REQUEST).run().getMessage();
        // compare the structure and values but not the attributes' order
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualJsonNode = mapper.readTree(getPayloadAsString(response));
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
