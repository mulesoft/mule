/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.junit.Test;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.transformer.types.MimeTypes;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SetVariableMimeTypeTestCase extends FunctionalTestCase {

    @Override
    protected String getConfigFile()
    {
        return "set-variable-mime-type-config.xml";
    }

    @Test
    public void setsMimeTypeOnVariableWithoutContentTypeOnPayload() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testInput1", TEST_MESSAGE, null);

        DataType dataType = (DataType) response.getPayload();

        assertThat(dataType, DataTypeMatcher.like(String.class, MimeTypes.APPLICATION_JSON, UTF_16.name()));
    }

    @Test
    public void setsContentTypeOnPayloadWithoutMimeTypeOnVariable() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testInput2", TEST_MESSAGE, null);
        response.setOutboundProperty("Content-Type", "application/json");

        String mimeType = response.getDataType().getMimeType();

        assertThat(mimeType, equalTo(MimeTypes.APPLICATION_JSON));
    }

    @Test
    public void requestWithoutAnyDataTypeSet() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testInput3", TEST_MESSAGE, null);

        String mimeType = response.getDataType().getMimeType();

        assertThat(mimeType, equalTo(MimeTypes.ANY));
        assertThat(mimeType, equalTo("*/*"));

    }

    public static class FlowVariableDataTypeAccessor implements Callable {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception {
            return eventContext.getMessage().getPropertyDataType("testVariable", PropertyScope.INVOCATION);
        }
    }
}
