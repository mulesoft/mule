/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonSchemaJsonValidationTestCase extends AbstractMuleContextTestCase
{
    private static final String EXPECTED_JSON =
            "{\n" +
            "  \"homeTeam\": \"BAR\",\n" +
            "  \"awayTeam\": \"RMA\",\n" +
            "  \"homeTeamScore\": 3,\n" +
            "  \"awayTeamScore\": 0\n" +
            "}";

    private static final String BAD_JSON =
            "{\n" +
            "  \"homeTeam\": \"BARCA\",\n" +
            "  \"awayTeam\": \"RMA\",\n" +
            "  \"homeTeamScore\": 3,\n" +
            "  \"awayTeamScore\": 0\n" +
            "}";

    private JsonSchemaValidationFilter filter;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        filter = new JsonSchemaValidationFilter();
        filter.setSchemaLocations("match-schema.json");
        filter.setMuleContext(muleContext);
        filter.initialise();
    }

    @Test
    public void filterShouldAcceptStringInput() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(EXPECTED_JSON, muleContext);
        boolean accepted = filter.accept(message);
        assertTrue(accepted);
        JSONAssert.assertEquals(EXPECTED_JSON, message.getPayloadAsString(), false);
    }

    @Test
    public void filterShouldAcceptReaderInput() throws Exception
    {
        StringReader reader = new StringReader(EXPECTED_JSON);
        MuleMessage message = new DefaultMuleMessage(reader, muleContext);
        boolean accepted = filter.accept(message);
        assertTrue(accepted);
        JSONAssert.assertEquals(EXPECTED_JSON, message.getPayloadAsString(), false);
    }

    @Test
    public void filterShouldAcceptByteArrayInput() throws Exception
    {
        byte[] bytes = EXPECTED_JSON.getBytes();
        MuleMessage message = new DefaultMuleMessage(bytes, muleContext);
        boolean accepted = filter.accept(message);
        assertTrue(accepted);
        JSONAssert.assertEquals(EXPECTED_JSON, message.getPayloadAsString(), false);
    }

    @Test
    public void filterShouldAcceptInputStreamInput() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new ByteArrayInputStream(EXPECTED_JSON.getBytes()), muleContext);
        boolean accepted = filter.accept(message);
        assertTrue(accepted);
        JSONAssert.assertEquals(EXPECTED_JSON, message.getPayloadAsString(), false);
    }

    @Test
    public void filterShouldNotAcceptInvalidJson() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(BAD_JSON, muleContext);
        boolean accepted = filter.accept(message);
        assertFalse(accepted);
    }

}
