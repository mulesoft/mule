/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerAttachmentsTestCase extends FunctionalTestCase
{

    public static final String TEXT_BODY_FIELD_NAME = "field1";
    public static final String TEXT_BODY_FIELD_VALUE = "yes";
    public static final String FILE_BODY_FIELD_NAME = "file";
    public static final String FILE_BODY_FIELD_VALUE = "someFileContent";
    public static final String FIELD_BDOY_FILE_NAME = "file.ext";
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty formDataPath = new SystemProperty("formDataPath", "formDataPath");
    @Rule
    public SystemProperty mixedPath = new SystemProperty("mixedPath", "mixedPath");
    @Rule
    public SystemProperty multipartResponse = new SystemProperty("multipartResponse", "multipartResponse");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-attachment-config.xml";
    }

    @Test
    @Ignore  // TODO MULE-8041: Fix HttpListenerAttachmentsTestCase
    public void receiveOnlyAttachmentsAndReturnOnlyAttachments() throws Exception
    {
        processAttachmentRequestAndResponse(formDataPath.getValue(), HttpHeaders.Values.MULTIPART_FORM_DATA);
    }

    @Test
    @Ignore  // TODO MULE-8041: Fix HttpListenerAttachmentsTestCase
    public void receiveOnlyAttachmentsAndReturnOnlyAttachmentsWithMultipartMixedResponse() throws Exception
    {
        processAttachmentRequestAndResponse(mixedPath.getValue(), "multipart/mixed");
    }

    private void processAttachmentRequestAndResponse(String pathToCall,  String expectedResponseContentType) throws IOException, MuleException, ServletException
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try
        {

            HttpPost httpPost = new HttpPost(String.format("http://localhost:%s/%s", listenPort.getNumber(), pathToCall));
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody(TEXT_BODY_FIELD_NAME, TEXT_BODY_FIELD_VALUE, ContentType.TEXT_PLAIN);
            builder.addBinaryBody(FILE_BODY_FIELD_NAME, FILE_BODY_FIELD_VALUE.getBytes(), ContentType.APPLICATION_OCTET_STREAM, FIELD_BDOY_FILE_NAME);
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            final CloseableHttpResponse response = httpClient.execute(httpPost);
            try
            {
                final MuleMessage receivedMessage = muleContext.getClient().request("vm://out", 1000);
                assertThat(receivedMessage.getPayload(), Is.<Object>is(NullPayload.getInstance()));
                assertThat(receivedMessage.getInboundAttachmentNames().size(), is(2));
                assertThat(receivedMessage.getInboundAttachmentNames().contains(TEXT_BODY_FIELD_NAME), is(true));
                assertThat(new String(((HttpPartDataSource) receivedMessage.getInboundAttachment(TEXT_BODY_FIELD_NAME).getDataSource()).getContent()), Is.<Object>is(TEXT_BODY_FIELD_VALUE));
                assertThat(receivedMessage.getInboundAttachmentNames().contains(FILE_BODY_FIELD_NAME), is(true));
                assertThat(new String(((HttpPartDataSource)receivedMessage.getInboundAttachment(FILE_BODY_FIELD_NAME).getDataSource()).getContent()), Is.<Object>is(FILE_BODY_FIELD_VALUE));

                final String contentType = response.getFirstHeader(HttpHeaders.Names.CONTENT_TYPE).getValue();
                assertThat(contentType, Matchers.containsString(expectedResponseContentType));

                final Collection<Part> parts = HttpParser.parseMultipartContent(response.getEntity().getContent(), contentType);
                assertThat(parts.size(), Is.is(2));
                Map<String, Part> partsAsMap = convertPartsToMap(parts);
                assertThat(partsAsMap.get(TEXT_BODY_FIELD_NAME), IsNull.notNullValue());
                assertThat(partsAsMap.get(FILE_BODY_FIELD_NAME), IsNull.notNullValue());
                assertThat(IOUtils.toString(partsAsMap.get(TEXT_BODY_FIELD_NAME).getInputStream()), is(TEXT_BODY_FIELD_VALUE));
                assertThat(IOUtils.toString(partsAsMap.get(FILE_BODY_FIELD_NAME).getInputStream()), is(FILE_BODY_FIELD_VALUE));
            }
            finally
            {
                response.close();
            }
        }
        finally
        {
            httpClient.close();
        }
    }

    private Map<String, Part> convertPartsToMap(Collection<Part> parts)
    {
        final Map<String, Part> partsAsMap = new HashMap<>();
        for (Part part : parts)
        {
            partsAsMap.put(part.getName(), part);
        }
        return partsAsMap;
    }


}