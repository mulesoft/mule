/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.transport.PropertyScope.OUTBOUND;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.client.OperationOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class MultipartMessageTestCase extends FunctionalTestCase
{

    private static final String UPLOADED_FILENAME = "uploadedFile";
    private static final String MULTIPART_BOUNDARY = "----------------------------299df9f9431b";
    private static final String MULTIPART_MESSAGE = "--" + MULTIPART_BOUNDARY + "\r\n" +
                                                    "Content-Disposition: form-data; name=\"" + UPLOADED_FILENAME + "\"; filename=\"upload.o\"\r\n" +
                                                    "Content-Type: application/octet-stream\r\n\r\n" +
                                                    "part payload\r\n\r\n" +
                                                    "--" + MULTIPART_BOUNDARY + "--\r\n\r\n";

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigResources()
    {
        return "multipart-message-config.xml";
    }

    @Test
    public void multiPartRequest() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage testMuleMessage = getTestMuleMessage(TEST_MESSAGE);
        testMuleMessage.setPayload(MULTIPART_MESSAGE);
        testMuleMessage.setProperty("Charset", UTF_8.name(), OUTBOUND);
        testMuleMessage.setProperty("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY, OUTBOUND);

        OperationOptions options = newOptions().method("POST").build();
        MuleMessage result = client.send(format("http://localhost:%d/", port.getNumber()), testMuleMessage, options);

        assertThat(result.getPayloadAsString(), is("Got " + UPLOADED_FILENAME));
    }
}
