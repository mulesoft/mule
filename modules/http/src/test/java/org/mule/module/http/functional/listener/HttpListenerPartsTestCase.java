/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.Boolean.getBoolean;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.http.Consts.ISO_8859_1;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import com.sun.mail.util.LineInputStream;

import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerPartsTestCase extends FunctionalTestCase
{

  private static final String MIME_UTF_PROPERTY = "mail.mime.allowutf8";
  private static final String MIME_PROPERTY_READ_FIELD_NAME = "defaultutf8";
  private static final String MIXED_CONTENT =
    "--the-boundary\r\n"
      + "Content-Type: text/plain; charset=ISO-8859-1\r\n"
      + "Content-Transfer-Encoding: 8bit\r\n"
      + "Content-Disposition: inline; name=\"field1\"; filename=\"£10.txt\" \r\n"
      + "\r\n"
      + "yes\r\n"
      + "--the-boundary--\r\n";

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public SystemProperty encoding = new SystemProperty(MIME_UTF_PROPERTY, "true");

  @Override
  protected String getConfigFile()
  {
    return "http-listener-parts-config.xml";
  }

  @Test
  public void utf8InHeaders() throws Exception
  {
    // The system property is read to a static field, so it will be set before this test executes
    Optional<Field> utf8Property = getMailProperty();
    Boolean previousValue = null;
    if (utf8Property.isPresent())
    {
      utf8Property.get().setAccessible(true);
      previousValue = utf8Property.get().getBoolean(null);
    }

    try (CloseableHttpClient httpClient = HttpClients.createDefault())
    {
      if (utf8Property.isPresent())
      {
        // Mimic the behavior of the property having been present at load time
        utf8Property.get().setBoolean(null, getBoolean(MIME_UTF_PROPERTY));
      }
      HttpPost httpPost = new HttpPost("http://localhost:" + listenPort.getValue() + "/utf");
      httpPost.setEntity(new ByteArrayEntity(MIXED_CONTENT.getBytes(), ContentType.create(
        "multipart/mixed", ISO_8859_1)
        .withParameters(new BasicNameValuePair("boundary", "the-boundary"))));
      try (CloseableHttpResponse response = httpClient.execute(httpPost))
      {
        assertThat(IOUtils.toString(response.getEntity().getContent()), is(MIXED_CONTENT));
      }
    }
    finally
    {
      if (previousValue != null)
      {
        utf8Property.get().setBoolean(null, previousValue);
      }
    }
  }

  private Optional<Field> getMailProperty()
  {
    try
    {
      return of(LineInputStream.class.getDeclaredField(MIME_PROPERTY_READ_FIELD_NAME));
    }
    catch (NoSuchFieldException e)
    {
      return empty();
    }
  }

}
