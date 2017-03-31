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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.builder;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.service.http.api.HttpConstants.Method.GET;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.service.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.service.http.api.HttpHeaders.Values.MULTIPART_FORM_DATA;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.HttpHeaders;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.entity.multipart.Part;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import com.google.common.collect.Lists;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
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
    HttpRequest request = HttpRequest.builder().setUri(getUrl(filePath.getValue())).setMethod(GET)
        .setEntity(new ByteArrayHttpEntity(TEST_PAYLOAD.getBytes())).build();
    final HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    final Collection<HttpPart> parts = parseMultipartContent(((InputStreamHttpEntity) response.getEntity()).getInputStream(),
                                                             response.getHeaderValueIgnoreCase(CONTENT_TYPE));
    assertThat(parts, hasSize(2));

    final Iterator<HttpPart> responsePartsIterator = parts.iterator();

    HttpPart part1 = responsePartsIterator.next();
    assertThat(part1.getContentType(), is(TEXT_PLAIN_LATIN.toRfcString()));
    assertThat(IOUtils.toString(part1.getInputStream()), is(TEXT_BODY_FIELD_VALUE));

    HttpPart part2 = responsePartsIterator.next();
    assertThat(part2.getFileName(), is(FILE_BODY_FIELD_FILENAME));
    assertThat(part2.getContentType(), is(BINARY.toRfcString()));
    assertThat(IOUtils.toString(part2.getInputStream()), is(FILE_BODY_FIELD_VALUE));
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

        final Collection<HttpPart> parts = parseMultipartContent(response.getEntity().getContent(), contentType);
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
        final Message receivedMessage = muleContext.getClient().request("test://out", 1000).getRight().get();
        assertThat(receivedMessage.getPayload().getValue(), instanceOf(MultiPartPayload.class));
        MultiPartPayload receivedParts = ((MultiPartPayload) receivedMessage.getPayload().getValue());
        assertThat(receivedParts.getParts().size(), is(2));
        assertThat(receivedParts.getPartNames(), hasItem(TEXT_BODY_FIELD_NAME));
        assertThat(receivedParts.getPartNames(), hasItem(FILE_BODY_FIELD_NAME));

        final String contentType = response.getFirstHeader(HttpHeaders.Names.CONTENT_TYPE).getValue();
        assertThat(contentType, containsString(expectedResponseContentType));

        final Collection<HttpPart> parts = parseMultipartContent(response.getEntity().getContent(), contentType);
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
      return Event.builder(event).message(of(new DefaultMultiPartPayload(part))).build();
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
      return Event.builder(event).message(of(new DefaultMultiPartPayload(part1, part2))).build();
    }
  }

  public static Collection<HttpPart> parseMultipartContent(InputStream content, String contentType) throws IOException {
    MimeMultipart mimeMultipart = null;
    List<HttpPart> parts = Lists.newArrayList();

    try {
      mimeMultipart = new MimeMultipart(new ByteArrayDataSource(content, contentType));
    } catch (MessagingException e) {
      throw new IOException(e);
    }

    try {
      int partCount = mimeMultipart.getCount();

      for (int i = 0; i < partCount; i++) {
        BodyPart part = mimeMultipart.getBodyPart(i);

        String filename = part.getFileName();
        String partName = filename;
        String[] contentDispositions = part.getHeader(CONTENT_DISPOSITION);
        if (contentDispositions != null) {
          String contentDisposition = contentDispositions[0];
          if (contentDisposition.contains("name")) {
            partName = contentDisposition.substring(contentDisposition.indexOf("name") + "name".length() + 2);
            partName = partName.substring(0, partName.indexOf("\""));
          }
        }
        HttpPart httpPart =
            new HttpPart(partName, filename, IOUtils.toByteArray(part.getInputStream()), part.getContentType(), part.getSize());

        Enumeration<Header> headers = part.getAllHeaders();

        while (headers.hasMoreElements()) {
          Header header = headers.nextElement();
          httpPart.addHeader(header.getName(), header.getValue());
        }
        parts.add(httpPart);
      }
    } catch (MessagingException e) {
      throw new IOException(e);
    }

    return parts;
  }
}
