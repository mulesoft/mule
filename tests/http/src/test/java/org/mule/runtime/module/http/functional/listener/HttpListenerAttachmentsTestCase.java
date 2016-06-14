/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.listener;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.MULTIPART_FORM_DATA;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.functional.AbstractHttpTestCase;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.multipart.HttpPart;
import org.mule.runtime.module.http.internal.multipart.HttpPartDataSource;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerAttachmentsTestCase extends AbstractHttpTestCase
{

    private static final String TEXT_BODY_FIELD_NAME = "field1";
    private static final String TEXT_BODY_FIELD_VALUE = "yes";
    private static final String FILE_BODY_FIELD_NAME = "file";
    //The value needs to be big enough to ensure several chunks if using transfer encoding chunked.
    private static final String FILE_BODY_FIELD_VALUE = randomAlphanumeric(1200000);
    private static final String FILE_BODY_FIELD_FILENAME = "file.ext";
    private static final boolean DO_NOT_USE_CHUNKED_MODE = false;
    private static final boolean USE_CHUNKED_MODE = true;
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty formDataPath = new SystemProperty("formDataPath", "formDataPath");
    @Rule
    public SystemProperty mixedPath = new SystemProperty("mixedPath", "mixedPath");
    @Rule
    public SystemProperty contentLength = new SystemProperty("contentLength", "contentLength");
    @Rule
    public SystemProperty chunked = new SystemProperty("chunked", "chunked");
    @Rule
    public SystemProperty filePath = new SystemProperty("filePath", "filePath");
    @Rule
    public SystemProperty formDataChunkedPath = new SystemProperty("multipartChunked", "multipartChunked");
    @Rule
    public SystemProperty multipartResponse = new SystemProperty("multipartResponse", "multipartResponse");


    @Override
    protected String getConfigFile()
    {
        return "http-listener-attachment-config.xml";
    }

    @Test
    public void receiveOnlyAttachmentsAndReturnOnlyAttachments() throws Exception
    {
        processAttachmentRequestAndResponse(formDataPath.getValue(), MULTIPART_FORM_DATA, DO_NOT_USE_CHUNKED_MODE);
    }

    @Test
    public void receiveOnlyAttachmentsAndReturnOnlyAttachmentsWithMultipartMixedResponse() throws Exception
    {
        processAttachmentRequestAndResponse(mixedPath.getValue(), "multipart/mixed", DO_NOT_USE_CHUNKED_MODE);
    }

    @Test
    public void receiveOnlyAttachmentsAndReturnOnlyAttachmentsWithMultipartFormDataAndTransferEncodingChunked() throws Exception
    {
        processAttachmentRequestAndResponse(formDataPath.getValue(), MULTIPART_FORM_DATA, USE_CHUNKED_MODE);
    }

    @Test
    public void respondWithAttachmentsContentLength() throws Exception
    {
        String contentLengthValue = getResponseWithExpectedAttachmentFrom(contentLength.getValue(), CONTENT_LENGTH);
        assertThat(contentLengthValue, is(notNullValue()));
    }

    @Test
    public void respondWithAttachmentsChunked() throws Exception
    {
        String transferEncodingValue = getResponseWithExpectedAttachmentFrom(chunked.getValue(), TRANSFER_ENCODING);
        assertThat(transferEncodingValue, is(CHUNKED));
    }

    @Test
    public void respondWithSeveralAttachments() throws Exception
    {
        MuleMessage response = muleContext.getClient().send(getUrl(filePath.getValue()), getTestMuleMessage());
        assertThat(response.getInboundAttachmentNames().size(), is(2));

        DataHandler attachment1 = response.getInboundAttachment(FILE_BODY_FIELD_NAME);
        HttpPart part = ((HttpPartDataSource) attachment1.getDataSource()).getPart();
        assertThat(part.getName(), is(FILE_BODY_FIELD_NAME));
        assertThat(part.getFileName(), is(FILE_BODY_FIELD_FILENAME));
        assertThat(part.getContentType(), is("application/octet-stream"));
        assertThat(IOUtils.toString(part.getInputStream()), is(FILE_BODY_FIELD_VALUE));

        DataHandler attachment2 = response.getInboundAttachment(TEXT_BODY_FIELD_NAME);
        assertThat((String) attachment2.getContent(), is(TEXT_BODY_FIELD_VALUE));
        assertThat(attachment2.getContentType(), is(TEXT_PLAIN.toString()));
    }

    @Test
    public void returnOnlyOneContentTypeHeaderPerPart() throws Exception
    {
        try (CloseableHttpClient httpClient = HttpClients.createDefault())
        {
            HttpPost httpPost = new HttpPost(getUrl(formDataPath.getValue()));
            httpPost.setEntity(getMultipartEntity(false));
            try (CloseableHttpResponse response = httpClient.execute(httpPost))
            {
                assertThat(countMatches(IOUtils.toString(response.getEntity().getContent()),CONTENT_TYPE), is(1));
            }
        }
    }

    private String getResponseWithExpectedAttachmentFrom(String path, String requiredHeader) throws MuleException, IOException
    {
        try (CloseableHttpClient httpClient = HttpClients.createDefault())
        {
            HttpGet httpGet = new HttpGet(getUrl(path));
            try (CloseableHttpResponse response = httpClient.execute(httpGet))
            {
                final String contentType = response.getFirstHeader(HttpHeaders.Names.CONTENT_TYPE).getValue();
                assertThat(contentType, containsString(MULTIPART_FORM_DATA));

                final Collection<HttpPart> parts = HttpParser.parseMultipartContent(response.getEntity().getContent(), contentType);
                assertThat(parts.size(), is(1));
                Map<String, Part> partsAsMap = convertPartsToMap(parts);
                assertThat(partsAsMap.get(TEXT_BODY_FIELD_NAME), notNullValue());
                assertThat(IOUtils.toString(partsAsMap.get(TEXT_BODY_FIELD_NAME).getInputStream()), is(TEXT_BODY_FIELD_VALUE));
                return response.getFirstHeader(requiredHeader).getValue();
            }
        }
    }

    private void processAttachmentRequestAndResponse(String pathToCall, String expectedResponseContentType, boolean useChunkedMode) throws IOException, MuleException, ServletException
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try
        {
            HttpPost httpPost = new HttpPost(getUrl(pathToCall));
            HttpEntity multipart = createHttpEntity(useChunkedMode);
            httpPost.setEntity(multipart);
            final CloseableHttpResponse response = httpClient.execute(httpPost);
            try
            {
                final MuleMessage receivedMessage = muleContext.getClient().request("test://out", 1000);
                assertThat(receivedMessage.getPayload(), instanceOf(HttpRequestAttributes.class));
                Map<String, DataHandler> receivedParts = ((HttpRequestAttributes) receivedMessage.getPayload()).getParts();
                assertThat(receivedParts.size(), is(2));
                assertThat(receivedParts.keySet(), hasItem(TEXT_BODY_FIELD_NAME));
                assertThat(new String(((HttpPartDataSource) receivedParts.get(TEXT_BODY_FIELD_NAME).getDataSource()).getContent()), is(TEXT_BODY_FIELD_VALUE));
                assertThat(receivedParts.keySet(), hasItem(FILE_BODY_FIELD_NAME));
                assertThat(new String(((HttpPartDataSource) receivedParts.get(FILE_BODY_FIELD_NAME).getDataSource()).getContent()), Is.<Object>is(FILE_BODY_FIELD_VALUE));

                final String contentType = response.getFirstHeader(HttpHeaders.Names.CONTENT_TYPE).getValue();
                assertThat(contentType, containsString(expectedResponseContentType));

                final Collection<HttpPart> parts = HttpParser.parseMultipartContent(response.getEntity().getContent(), contentType);
                assertThat(parts.size(), is(2));
                Map<String, Part> partsAsMap = convertPartsToMap(parts);
                assertThat(partsAsMap.get(TEXT_BODY_FIELD_NAME), notNullValue());
                assertThat(partsAsMap.get(FILE_BODY_FIELD_NAME), notNullValue());
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

    private HttpEntity createHttpEntity(boolean useChunkedMode) throws IOException
    {
        HttpEntity multipartEntity = getMultipartEntity(true);
        if (useChunkedMode)
        {
            //The only way to send multipart + chunked is putting the multipart content in an output stream entity.
            ByteArrayOutputStream multipartOutput = new ByteArrayOutputStream();
            multipartEntity.writeTo(multipartOutput);
            multipartOutput.flush();
            ByteArrayEntity byteArrayEntity = new ByteArrayEntity(multipartOutput.toByteArray());
            multipartOutput.close();

            byteArrayEntity.setChunked(true);
            byteArrayEntity.setContentEncoding(multipartEntity.getContentEncoding());
            byteArrayEntity.setContentType(multipartEntity.getContentType());
            return byteArrayEntity;
        }
        else
        {
            return multipartEntity;
        }
    }

    private HttpEntity getMultipartEntity(boolean withFile)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody(TEXT_BODY_FIELD_NAME, TEXT_BODY_FIELD_VALUE, TEXT_PLAIN);
        if (withFile)
        {
            builder.addBinaryBody(FILE_BODY_FIELD_NAME, FILE_BODY_FIELD_VALUE.getBytes(), APPLICATION_OCTET_STREAM, FILE_BODY_FIELD_FILENAME);
        }
        return builder.build();
    }

    private String getUrl(String pathToCall)
    {
        return format("http://localhost:%s/%s", listenPort.getNumber(), pathToCall);
    }

    private Map<String, Part> convertPartsToMap(Collection<HttpPart> parts)
    {
        final Map<String, Part> partsAsMap = new HashMap<>();
        for (Part part : parts)
        {
            partsAsMap.put(part.getName(), part);
        }
        return partsAsMap;
    }

    public static class CreatePartMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            org.mule.extension.http.api.HttpPart part = new org.mule.extension.http.api.HttpPart(TEXT_BODY_FIELD_NAME,
                                                                                                 TEXT_BODY_FIELD_VALUE,
                                                                                                 TEXT_PLAIN.toString(),
                                                                                                 null);
            event.setFlowVariable("parts", asList(part));
            return event;
        }
    }

    public static class CreatePartsMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            org.mule.extension.http.api.HttpPart part1 = new org.mule.extension.http.api.HttpPart(TEXT_BODY_FIELD_NAME,
                                                                                                  TEXT_BODY_FIELD_VALUE,
                                                                                                  TEXT_PLAIN.toString(),
                                                                                                  null);
            org.mule.extension.http.api.HttpPart part2 = new org.mule.extension.http.api.HttpPart(FILE_BODY_FIELD_NAME,
                                                                                                  FILE_BODY_FIELD_VALUE.getBytes(),
                                                                                                  APPLICATION_OCTET_STREAM.toString(),
                                                                                                  FILE_BODY_FIELD_FILENAME);
            event.setFlowVariable("parts", asList(part1, part2));
            return event;
        }
    }

    public static class ConvertPartsMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            List<org.mule.extension.http.api.HttpPart> parts = new LinkedList<>();
            ((HttpRequestAttributes) event.getMessage().getAttributes())
                    .getParts().forEach((id, dataHandler) ->
                                        {
                                            try
                                            {
                                                String filename = null;
                                                if (!id.equals(dataHandler.getName()))
                                                {
                                                    filename = dataHandler.getName();
                                                }
                                                parts.add(new org.mule.extension.http.api.HttpPart(id,
                                                                       dataHandler.getContent(),
                                                                       dataHandler.getContentType(),
                                                                       filename));
                                            }
                                            catch (IOException e)
                                            {
                                                //do nothing
                                            }
                                        }
            );
            event.setFlowVariable("parts", parts);
            return event;
        }
    }
}