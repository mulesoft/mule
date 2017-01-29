/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.module.cxf;


import static org.custommonkey.xmlunit.XMLUnit.compareXML;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_ENCODING;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GZIPEncodingTestCase extends AbstractCxfOverHttpExtensionTestCase {

  private static final String GZIP = "gzip";

  @Rule
  public final DynamicPort httpPort = new DynamicPort("port1");

  @Rule
  public final DynamicPort httpPortProxy = new DynamicPort("port2");

  private String getAllRequest;
  private String getAllResponse;
  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  @Before
  public void doSetUp() throws Exception {
    getAllRequest = IOUtils.getResourceAsString("artistregistry-get-all-request.xml", getClass());
    getAllResponse = IOUtils.getResourceAsString("artistregistry-get-all-response.xml", getClass());
    XMLUnit.setIgnoreWhitespace(true);
  }

  @Override
  protected String getConfigFile() {
    return "gzip-encoding-conf-httpn.xml";
  }

  @Test
  public void proxyWithGZIPResponse() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + httpPortProxy.getNumber() + "/proxy")
            .setMethod(POST.name())
            .setEntity(new ByteArrayHttpEntity(getAllRequest.getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    validateResponse(response);
  }

  @Test
  public void proxyWithGZIPRequestAndResponse() throws Exception {
    ParameterMap headersMap = new ParameterMap();
    headersMap.put(CONTENT_ENCODING, "gzip,deflate");

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + httpPortProxy.getNumber() + "/proxy")
            .setMethod(POST.name())
            .setHeaders(headersMap)
            .setEntity(new ByteArrayHttpEntity(gzip(getAllRequest))).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    validateResponse(response);
  }

  private void validateResponse(HttpResponse response) throws Exception {
    String unzipped = unzip(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertThat(unzipped, compareXML(getAllResponse, unzipped).identical(), is(true));
    assertThat(response.getHeaderValue("content-encoding"), is(GZIP));
  }

  private String unzip(InputStream input) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(input);
    InputStreamReader reader = new InputStreamReader(gzip);
    StringWriter writer = new StringWriter();

    char[] buffer = new char[10240];
    int length;
    while ((length = reader.read(buffer)) > 0) {
      writer.write(buffer, 0, length);
    }

    writer.close();
    return writer.toString();
  }

  private byte[] gzip(String input) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    GZIPOutputStream gzip = new GZIPOutputStream(os);
    gzip.write(input.getBytes());
    gzip.close();
    return os.toByteArray();
  }
}
