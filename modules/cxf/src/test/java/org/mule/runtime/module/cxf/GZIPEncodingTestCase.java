/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.cxf;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_ENCODING;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GZIPEncodingTestCase extends FunctionalTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).build();

  private static final String GZIP = "gzip";

  @Rule
  public final DynamicPort httpPort = new DynamicPort("port1");

  @Rule
  public final DynamicPort httpPortProxy = new DynamicPort("port2");

  private String getAllRequest;
  private String getAllResponse;

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
    MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/proxy",
                                                        getTestMuleMessage(getAllRequest), HTTP_REQUEST_OPTIONS)
        .getRight();
    validateResponse(response);
  }

  @Test
  public void proxyWithGZIPRequestAndResponse() throws Exception {
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(CONTENT_ENCODING, "gzip,deflate");
    MuleMessage response = muleContext.getClient()
        .send("http://localhost:" + httpPortProxy.getNumber() + "/proxy",
              MuleMessage.builder().payload(gzip(getAllRequest)).outboundProperties(properties).build(), HTTP_REQUEST_OPTIONS)
        .getRight();
    validateResponse(response);
  }

  private void validateResponse(MuleMessage response) throws Exception {
    String unzipped = unzip(new ByteArrayInputStream(getPayloadAsBytes(response)));
    assertTrue(XMLUnit.compareXML(getAllResponse, unzipped).identical());
    assertEquals(GZIP, response.getInboundProperty(CONTENT_ENCODING));
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
