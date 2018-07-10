/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TRANSFER_ENCODING;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.module.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.module.http.api.HttpHeaders.Values.MULTIPART_FORM_DATA;
import static org.mule.module.http.internal.HttpParser.parseMultipartContent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.message.ds.ByteArrayDataSource;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.multipart.HttpPart;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.module.http.internal.request.grizzly.ByteArrayPart;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerAttachmentsTestCase extends FunctionalTestCase
{

    private static final String TEXT_BODY_FIELD_NAME = "field1";
    private static final String TEXT_BODY_FIELD_VALUE = "yes";
    private static final String FILE_BODY_FIELD_NAME = "file";
    //The value needs to be big enough to ensure several chunks if using transfer encoding chunked.
    private static final String FILE_BODY_FIELD_VALUE = randomAlphanumeric(1200000);
    private static final String FIELD_BDOY_FILE_NAME = "file.ext";
    private static final String VM_MESSAGE_ENDPOINT = "vm://out";
    private static final boolean DO_NOT_USE_CHUNKED_MODE = false;
    private static final boolean USE_CHUNKED_MODE = true;
    private static final String TEXT_PLAIN = "text/plain";
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

    private static Set<String> inboundAttachmentNames;

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
    public void receiveAttachmentWithCustomHeaders() throws Exception
    {

        RequestBuilder requestBuilder = new RequestBuilder();
        AsyncHttpClientConfig asyncHttpClientConfig = new AsyncHttpClientConfig.Builder().build();
        GrizzlyAsyncHttpProvider grizzlyAsyncHttpProvider = new GrizzlyAsyncHttpProvider(asyncHttpClientConfig);

        requestBuilder.setMethod("POST");
        requestBuilder.setUrl(getUrl(formDataPath.getValue()));
        ByteArrayPart byteArrayPart = new ByteArrayPart("partName", "blahblah".getBytes(), "application/pdf", null, null, null, "base64");
        byteArrayPart.setCustomContentDisposition("file; name=\"partName\"; documentId=1");
        byteArrayPart.addCustomHeader("Custom-Header: ", "custom");
        requestBuilder.addBodyPart(byteArrayPart);

        try (AsyncHttpClient asyncHttpClient = new AsyncHttpClient(grizzlyAsyncHttpProvider, asyncHttpClientConfig))
        {

            ListenableFuture<Response> responseFuture = asyncHttpClient.executeRequest(requestBuilder.build());
            com.ning.http.client.Response response = responseFuture.get();

            // check that the Mule message is created correctly from the HTTP request
            final MuleMessage receivedMessage = muleContext.getClient().request(VM_MESSAGE_ENDPOINT, 1000);
            assertThat(receivedMessage.getInboundAttachmentNames().size(), is(1));
            HttpPartDataSource muleAttachment = (HttpPartDataSource) receivedMessage.getInboundAttachment("partName").getDataSource();
            assertThat(muleAttachment.getHeader("Custom-Header"), equalTo("custom"));
            assertThat(muleAttachment.getContentType(), equalTo("application/pdf"));
            assertThat(muleAttachment.getHeader(CONTENT_TRANSFER_ENCODING), equalTo("base64"));
            assertThat(muleAttachment.getHeader(CONTENT_DISPOSITION), equalTo("file; name=\"partName\"; documentId=1"));

            // check that the HTTP response includes the same headers as the HTTP request (when echoing)
            final Collection<HttpPart> parts = parseMultipartContent(response.getResponseBodyAsStream(), response.getContentType());
            assertThat(parts.size(), is(1));
            HttpPart receivedPart = new ArrayList<>(parts).get(0);
            assertThat(receivedPart.getHeader("Custom-Header"), equalTo("custom"));
            assertThat(receivedPart.getContentType(), equalTo("application/pdf"));
            assertThat(receivedPart.getHeader(CONTENT_TRANSFER_ENCODING), equalTo("base64"));
            assertThat(receivedPart.getHeader(CONTENT_DISPOSITION), equalTo("file; name=\"partName\"; documentId=1"));
        }
    }

    @Test
    public void receiveAndReturnMultipleAttachments() throws Exception
    {
        RequestBuilder requestBuilder = new RequestBuilder();
        AsyncHttpClientConfig asyncHttpClientConfig = new AsyncHttpClientConfig.Builder().build();
        GrizzlyAsyncHttpProvider grizzlyAsyncHttpProvider = new GrizzlyAsyncHttpProvider(asyncHttpClientConfig);

        requestBuilder.setMethod("POST");
        requestBuilder.setUrl(getUrl("multiple"));

        requestBuilder.setHeader(CONTENT_TYPE, "multipart/form-data");
        ByteArrayPart byteArrayPart = new ByteArrayPart("another", "no".getBytes(), "text/plain", null, null, null, null);
        requestBuilder.addBodyPart(byteArrayPart);
        ByteArrayPart byteArrayPart2 = new ByteArrayPart(TEXT_BODY_FIELD_NAME, TEXT_BODY_FIELD_VALUE.getBytes(), "text/plain", null, null, null, null);
        requestBuilder.addBodyPart(byteArrayPart2);

        try (AsyncHttpClient asyncHttpClient = new AsyncHttpClient(grizzlyAsyncHttpProvider, asyncHttpClientConfig))
        {
            ListenableFuture<Response> responseFuture = asyncHttpClient.executeRequest(requestBuilder.build());
            com.ning.http.client.Response response = responseFuture.get();

            assertThat(inboundAttachmentNames, hasSize(2));
            Iterator<String> iterator = inboundAttachmentNames.iterator();
            assertThat(iterator.next(), is("another"));
            assertThat(iterator.next(), is(TEXT_BODY_FIELD_NAME));

            final Collection<HttpPart> parts = parseMultipartContent(response.getResponseBodyAsStream(), response.getContentType());
            assertThat(parts.size(), is(2));
            HttpPart receivedPart1 = new ArrayList<>(parts).get(0);
            assertThat(receivedPart1.getContentType(), equalTo(TEXT_PLAIN));
            assertThat(receivedPart1.getHeader(CONTENT_DISPOSITION), equalTo("form-data; name=\"another\""));
            HttpPart receivedPart2 = new ArrayList<>(parts).get(1);
            assertThat(receivedPart2.getContentType(), equalTo(TEXT_PLAIN));
            assertThat(receivedPart2.getHeader(CONTENT_DISPOSITION), equalTo("form-data; name=\"field1\""));
        }
    }

    @Test
    public void respondWithAttachmentsContentLength() throws Exception
    {
        MuleMessage response = getResponseWithExpectedAttachmentFrom(contentLength.getValue());
        assertThat(response.getInboundProperty(CONTENT_LENGTH), is(notNullValue()));
    }

    @Test
    public void respondWithAttachmentsChunked() throws Exception
    {
        MuleMessage response = getResponseWithExpectedAttachmentFrom(chunked.getValue());
        assertThat((String) response.getInboundProperty(TRANSFER_ENCODING), is(CHUNKED));
    }

    @Test
    public void respondWithSeveralAttachments() throws Exception
    {
        MuleMessage response = muleContext.getClient().send(getUrl(filePath.getValue()), getTestMuleMessage());
        assertThat(response.getInboundAttachmentNames().size(), is(2));

        DataHandler attachment1 = response.getInboundAttachment(FILE_BODY_FIELD_NAME);
        HttpPart part = ((HttpPartDataSource) attachment1.getDataSource()).getPart();
        assertThat(part.getName(), is(FILE_BODY_FIELD_NAME));
        assertThat(part.getFileName(), is(FIELD_BDOY_FILE_NAME));
        assertThat(part.getContentType(), is("application/octet-stream"));
        assertThat(IOUtils.toString(part.getInputStream()), is(FILE_BODY_FIELD_VALUE));

        DataHandler attachment2 = response.getInboundAttachment(TEXT_BODY_FIELD_NAME);
        assertThat((String) attachment2.getContent(), is(TEXT_BODY_FIELD_VALUE));
        assertThat(attachment2.getContentType(), is(TEXT_PLAIN));
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

    @Test
    public void receiveMultipartMixed() throws Exception {
        RequestBuilder requestBuilder = new RequestBuilder();
        AsyncHttpClientConfig asyncHttpClientConfig = new AsyncHttpClientConfig.Builder().build();
        GrizzlyAsyncHttpProvider grizzlyAsyncHttpProvider = new GrizzlyAsyncHttpProvider(asyncHttpClientConfig);

        requestBuilder.setMethod("POST");
        requestBuilder.setUrl(getUrl(mixedPath.getValue()));
        requestBuilder.setHeader(CONTENT_TYPE, "multipart/mixed");
        ByteArrayPart byteArrayPart = new ByteArrayPart(null, "blahblah".getBytes(), "application/pdf", null, null, null, "base64");
        byteArrayPart.addCustomHeader("Custom-Header: ", "custom");
        requestBuilder.addBodyPart(byteArrayPart);

        try (AsyncHttpClient asyncHttpClient = new AsyncHttpClient(grizzlyAsyncHttpProvider, asyncHttpClientConfig))
        {

            ListenableFuture<Response> responseFuture = asyncHttpClient.executeRequest(requestBuilder.build());
            com.ning.http.client.Response response = responseFuture.get();

            // check that the Mule message is created correctly from the HTTP request
            final MuleMessage receivedMessage = muleContext.getClient().request(VM_MESSAGE_ENDPOINT, 1000);
            assertThat(receivedMessage.getInboundAttachmentNames().size(), is(1));
            HttpPartDataSource muleAttachment = (HttpPartDataSource) receivedMessage.getInboundAttachment("mule_attachment_0").getDataSource();
            assertThat(muleAttachment.getHeader("Custom-Header"), equalTo("custom"));
            assertThat(muleAttachment.getContentType(), equalTo("application/pdf"));
            assertThat(muleAttachment.getHeader(CONTENT_TRANSFER_ENCODING), equalTo("base64"));

            // check that the HTTP response includes the same headers as the HTTP request (when echoing)
            final Collection<HttpPart> parts = parseMultipartContent(response.getResponseBodyAsStream(), response.getContentType());
            assertThat(parts.size(), is(1));
            HttpPart receivedPart = new ArrayList<>(parts).get(0);
            assertThat(receivedPart.getHeader("Custom-Header"), equalTo("custom"));
            assertThat(receivedPart.getContentType(), equalTo("application/pdf"));
            assertThat(receivedPart.getHeader(CONTENT_TRANSFER_ENCODING), equalTo("base64"));
        }
    }

    private MuleMessage getResponseWithExpectedAttachmentFrom(String path) throws MuleException, IOException
    {
        MuleMessage response = muleContext.getClient().send(getUrl(path), getTestMuleMessage());
        assertThat(response.getInboundAttachmentNames().size(), is(1));
        DataHandler attachment = response.getInboundAttachment(TEXT_BODY_FIELD_NAME);
        assertThat((String) attachment.getContent(), is(TEXT_BODY_FIELD_VALUE));
        assertThat(attachment.getContentType(), is(TEXT_PLAIN));
        return response;
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
                final MuleMessage receivedMessage = muleContext.getClient().request(VM_MESSAGE_ENDPOINT, 1000);
                assertThat(receivedMessage.getPayload(), Is.<Object>is(NullPayload.getInstance()));
                assertThat(receivedMessage.getInboundAttachmentNames().size(), is(2));
                assertThat(receivedMessage.getInboundAttachmentNames().contains(TEXT_BODY_FIELD_NAME), is(true));
                assertThat(new String(((HttpPartDataSource) receivedMessage.getInboundAttachment(TEXT_BODY_FIELD_NAME).getDataSource()).getContent()), Is.<Object>is(TEXT_BODY_FIELD_VALUE));
                assertThat(receivedMessage.getInboundAttachmentNames().contains(FILE_BODY_FIELD_NAME), is(true));
                assertThat(new String(((HttpPartDataSource) receivedMessage.getInboundAttachment(FILE_BODY_FIELD_NAME).getDataSource()).getContent()), Is.<Object>is(FILE_BODY_FIELD_VALUE));

                final String contentType = response.getFirstHeader(HttpHeaders.Names.CONTENT_TYPE).getValue();
                assertThat(contentType, Matchers.containsString(expectedResponseContentType));

                final Collection<HttpPart> parts = parseMultipartContent(response.getEntity().getContent(), contentType);
                assertThat(parts.size(), is(2));
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
        builder.addTextBody(TEXT_BODY_FIELD_NAME, TEXT_BODY_FIELD_VALUE, ContentType.TEXT_PLAIN);
        if (withFile)
        {
            builder.addBinaryBody(FILE_BODY_FIELD_NAME, FILE_BODY_FIELD_VALUE.getBytes(), ContentType.APPLICATION_OCTET_STREAM, FIELD_BDOY_FILE_NAME);
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

    public static class CustomAttachmentMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                event.getMessage().addOutboundAttachment(FILE_BODY_FIELD_NAME, new DataHandler(new ByteArrayDataSource(FILE_BODY_FIELD_VALUE.getBytes(), ContentType.APPLICATION_OCTET_STREAM.toString(), FIELD_BDOY_FILE_NAME)));
            }
            catch (Exception e)
            {
                //do nothing
            }
            return event;
        }
    }

    public static class GatherAttachmentsMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            inboundAttachmentNames = event.getMessage().getInboundAttachmentNames();
            return event;
        }
    }
}
