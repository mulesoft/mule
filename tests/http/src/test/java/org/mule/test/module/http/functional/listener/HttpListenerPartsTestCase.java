/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasAttributes;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasMediaType;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.MultiPartPayloadMatchers.hasSize;
import static org.mule.functional.junit4.matchers.PartAttributesMatchers.hasFilename;
import static org.mule.functional.junit4.matchers.PartAttributesMatchers.hasName;
import static org.mule.functional.junit4.matchers.ThatMatcher.that;
import static org.mule.runtime.api.message.Message.builder;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.MULTIPART_FORM_DATA;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.entity.multipart.Part;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerPartsTestCase extends AbstractHttpTestCase {

  private static final String TEXT_BODY_FIELD_NAME = "field1";
  private static final String TEXT_BODY_FIELD_VALUE = "yes";
  private static final String FILE_BODY_FIELD_NAME = "file";
  // The value needs to be big enough to ensure several chunks if using transfer encoding chunked.
  private static final String FILE_BODY_FIELD_VALUE = randomAlphanumeric(1200000);
  private static final String FILE_BODY_FIELD_FILENAME = "file.ext";
  private static final MediaType TEXT_PLAIN_LATIN = MediaType.create("text", "plain", ISO_8859_1);
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
  protected String getConfigFile() {
    return "http-listener-attachment-config.xml";
  }

  @Test
  public void receiveOnlyAttachmentsAndReturnOnlyAttachments() throws Exception {
    processAttachmentRequestAndResponse(formDataPath.getValue(), MULTIPART_FORM_DATA, DO_NOT_USE_CHUNKED_MODE);
  }

  @Test
  public void receiveOnlyAttachmentsAndReturnOnlyAttachmentsWithMultipartMixedResponse() throws Exception {
    processAttachmentRequestAndResponse(mixedPath.getValue(), "multipart/mixed", DO_NOT_USE_CHUNKED_MODE);
  }

  @Test
  public void receiveOnlyAttachmentsAndReturnOnlyAttachmentsWithMultipartFormDataAndTransferEncodingChunked() throws Exception {
    processAttachmentRequestAndResponse(formDataPath.getValue(), MULTIPART_FORM_DATA, USE_CHUNKED_MODE);
  }

  @Test
  public void respondWithAttachmentsContentLength() throws Exception {
    String contentLengthValue = getResponseWithExpectedAttachmentFrom(contentLength.getValue(), CONTENT_LENGTH);
    assertThat(contentLengthValue, is(notNullValue()));
  }

  @Test
  public void respondWithAttachmentsChunked() throws Exception {
    String transferEncodingValue = getResponseWithExpectedAttachmentFrom(chunked.getValue(), TRANSFER_ENCODING);
    assertThat(transferEncodingValue, is(CHUNKED));
  }

  @Test
  public void respondWithSeveralAttachments() throws Exception {
    InternalMessage response =
        muleContext.getClient().send(getUrl(filePath.getValue()), InternalMessage.of(TEST_PAYLOAD)).getRight();
    assertThat(response.getPayload().getValue(), instanceOf(MultiPartPayload.class));
    assertThat((MultiPartPayload) response.getPayload().getValue(), hasSize(2));

    Message attachment1 = ((MultiPartPayload) response.getPayload().getValue()).getPart(FILE_BODY_FIELD_NAME);
    assertThat(attachment1, hasAttributes(that(hasName(FILE_BODY_FIELD_NAME))));
    assertThat(attachment1, hasAttributes(that(hasFilename(FILE_BODY_FIELD_FILENAME))));
    assertThat(attachment1, hasMediaType(BINARY));
    assertThat(attachment1, hasPayload(equalTo(FILE_BODY_FIELD_VALUE)));

    Message attachment2 = ((MultiPartPayload) response.getPayload().getValue()).getPart(TEXT_BODY_FIELD_NAME);
    assertThat(attachment2, hasPayload(equalTo(TEXT_BODY_FIELD_VALUE)));
    assertThat(attachment2, hasMediaType(TEXT_PLAIN_LATIN));
  }

  @Test
  public void returnOnlyOneContentTypeHeaderPerPart() throws Exception {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(getUrl(formDataPath.getValue()));
      httpPost.setEntity(getMultipartEntity(false));
      try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        assertThat(countMatches(IOUtils.toString(response.getEntity().getContent()), CONTENT_TYPE), is(1));
      }
    }
  }

  private String getResponseWithExpectedAttachmentFrom(String path, String requiredHeader) throws MuleException, IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet(getUrl(path));
      try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
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

  private void processAttachmentRequestAndResponse(String pathToCall, String expectedResponseContentType, boolean useChunkedMode)
      throws IOException, MuleException, ServletException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    try {
      HttpPost httpPost = new HttpPost(getUrl(pathToCall));
      HttpEntity multipart = createHttpEntity(useChunkedMode);
      httpPost.setEntity(multipart);
      final CloseableHttpResponse response = httpClient.execute(httpPost);
      try {
        final InternalMessage receivedMessage = muleContext.getClient().request("test://out", 1000).getRight().get();
        assertThat(receivedMessage.getPayload().getValue(), instanceOf(MultiPartPayload.class));
        MultiPartPayload receivedParts = ((MultiPartPayload) receivedMessage.getPayload().getValue());
        assertThat(receivedParts.getParts().size(), is(2));
        assertThat(receivedParts.getPartNames(), hasItem(TEXT_BODY_FIELD_NAME));
        assertThat(receivedParts.getPartNames(), hasItem(FILE_BODY_FIELD_NAME));

        final String contentType = response.getFirstHeader(HttpHeaders.Names.CONTENT_TYPE).getValue();
        assertThat(contentType, containsString(expectedResponseContentType));

        final Collection<HttpPart> parts = HttpParser.parseMultipartContent(response.getEntity().getContent(), contentType);
        assertThat(parts.size(), is(2));
        Map<String, Part> partsAsMap = convertPartsToMap(parts);
        assertThat(partsAsMap.get(TEXT_BODY_FIELD_NAME), notNullValue());
        assertThat(partsAsMap.get(FILE_BODY_FIELD_NAME), notNullValue());
        assertThat(IOUtils.toString(partsAsMap.get(TEXT_BODY_FIELD_NAME).getInputStream()), is(TEXT_BODY_FIELD_VALUE));
        assertThat(IOUtils.toString(partsAsMap.get(FILE_BODY_FIELD_NAME).getInputStream()), is(FILE_BODY_FIELD_VALUE));
      } finally {
        response.close();
      }
    } finally {
      httpClient.close();
    }
  }

  private HttpEntity createHttpEntity(boolean useChunkedMode) throws IOException {
    HttpEntity multipartEntity = getMultipartEntity(true);
    if (useChunkedMode) {
      // The only way to send multipart + chunked is putting the multipart content in an output stream entity.
      ByteArrayOutputStream multipartOutput = new ByteArrayOutputStream();
      multipartEntity.writeTo(multipartOutput);
      multipartOutput.flush();
      ByteArrayEntity byteArrayEntity = new ByteArrayEntity(multipartOutput.toByteArray());
      multipartOutput.close();

      byteArrayEntity.setChunked(true);
      byteArrayEntity.setContentEncoding(multipartEntity.getContentEncoding());
      byteArrayEntity.setContentType(multipartEntity.getContentType());
      return byteArrayEntity;
    } else {
      return multipartEntity;
    }
  }

  private HttpEntity getMultipartEntity(boolean withFile) {
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addTextBody(TEXT_BODY_FIELD_NAME, TEXT_BODY_FIELD_VALUE, TEXT_PLAIN);
    if (withFile) {
      builder.addBinaryBody(FILE_BODY_FIELD_NAME, FILE_BODY_FIELD_VALUE.getBytes(), APPLICATION_OCTET_STREAM,
                            FILE_BODY_FIELD_FILENAME);
    }
    return builder.build();
  }

  private String getUrl(String pathToCall) {
    return format("http://localhost:%s/%s", listenPort.getNumber(), pathToCall);
  }

  private Map<String, Part> convertPartsToMap(Collection<HttpPart> parts) {
    final Map<String, Part> partsAsMap = new HashMap<>();
    for (Part part : parts) {
      partsAsMap.put(part.getName(), part);
    }
    return partsAsMap;
  }

  public static class CreatePartMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      PartAttributes partAttributes = new PartAttributes(TEXT_BODY_FIELD_NAME);
      Message part = builder().payload(TEXT_BODY_FIELD_VALUE).attributes(partAttributes).mediaType(TEXT_PLAIN_LATIN).build();
      return Event.builder(event).message(InternalMessage.of(new DefaultMultiPartPayload(part))).build();
    }
  }

  public static class CreatePartsMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      PartAttributes part1Attributes = new PartAttributes(TEXT_BODY_FIELD_NAME);
      Message part1 = builder().payload(TEXT_BODY_FIELD_VALUE).attributes(part1Attributes).mediaType(TEXT_PLAIN_LATIN).build();
      PartAttributes part2Attributes = new PartAttributes(FILE_BODY_FIELD_NAME,
                                                          FILE_BODY_FIELD_FILENAME,
                                                          FILE_BODY_FIELD_VALUE.length(),
                                                          emptyMap());
      Message part2 = builder().payload(FILE_BODY_FIELD_VALUE).attributes(part2Attributes).mediaType(BINARY).build();
      return Event.builder(event).message(InternalMessage.of(new DefaultMultiPartPayload(part1, part2))).build();
    }
  }

}
