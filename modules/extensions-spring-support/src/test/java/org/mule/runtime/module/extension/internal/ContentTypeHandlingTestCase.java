/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.nio.charset.Charset.availableCharsets;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ContentTypeHandlingTestCase extends ExtensionFunctionalTestCase {

  private static Charset customEncoding;

  @Rule
  public SystemProperty customEncodingProperty = new SystemProperty("customEncoding", customEncoding.name());

  @Override
  protected String getConfigFile() {
    return "content-type-handling-config.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {HeisenbergExtension.class};
  }

  @BeforeClass
  public static void before() throws Exception {
    customEncoding = defaultCharset().name().equals(UTF_8) ? ISO_8859_1 : UTF_8;
  }

  @Test
  public void setsContentTypeOnXml() throws Exception {
    MuleEvent response = runFlow("setsContentTypeOnXml");
    DataType dataType = response.getMessage().getDataType();
    assertCustomEncoding(dataType);
    assertThat(dataType.getMediaType().getPrimaryType(), is(MediaType.TEXT.getPrimaryType()));
    assertThat(dataType.getMediaType().getSubType(), is(MediaType.TEXT.getSubType()));
  }

  @Test
  public void onlySetEncodingOnXml() throws Exception {
    MuleEvent response = runFlow("onlySetEncodingOnXml");
    DataType dataType = response.getMessage().getDataType();
    assertCustomEncoding(dataType);
    assertCustomMimeType(dataType);
  }

  @Test
  public void onlySetMimeTypeOnXml() throws Exception {
    MuleEvent response = runFlow("onlySetMimeTypeOnXml");
    DataType dataType = response.getMessage().getDataType();
    assertDefaultEncoding(dataType);
    assertCustomMimeType(dataType);
  }

  @Test
  public void maintainsContentType() throws Exception {
    MuleEvent response = flowRunner("defaultContentType").withPayload("").run();
    final DataType responseDataType = response.getMessage().getDataType();

    assertDefaultEncoding(responseDataType);
    assertDefaultMimeType(responseDataType);
  }

  @Test
  public void setEncodingInMimeTypeAndParam() throws Exception {
    MuleEvent response = runFlow("setEncodingInMimeTypeAndParam");
    DataType dataType = response.getMessage().getDataType();
    assertThat(dataType.getMediaType().getPrimaryType(), is("application"));
    assertThat(dataType.getMediaType().getSubType(), is("json"));
    assertThat(dataType.getMediaType().getCharset().get(), is(StandardCharsets.UTF_16));
  }

  @Test
  public void overridesContentType() throws Exception {
    Charset lastSupportedEncoding = availableCharsets().values().stream().reduce((first, last) -> last).get();
    MuleEvent response = runFlow("setsContentTypeProgrammatically");

    final DataType dataType = response.getMessage().getDataType();
    assertCustomMimeType(dataType);
    assertThat(dataType.getMediaType().getCharset().get(), is(lastSupportedEncoding));
  }

  private void assertCustomMimeType(DataType dataType) {
    assertThat(dataType.getMediaType().getPrimaryType(), is("dead"));
    assertThat(dataType.getMediaType().getSubType(), is("dead"));
  }

  private void assertCustomEncoding(DataType dataType) {
    assertThat(dataType.getMediaType().getCharset().get(), is(customEncoding));
  }

  private void assertDefaultEncoding(DataType dataType) throws Exception {
    assertThat(dataType.getMediaType().getCharset().get(), is(getDefaultEncoding(muleContext)));
  }

  private void assertDefaultMimeType(DataType dataType) throws Exception {
    assertThat(dataType.getMediaType().getPrimaryType(), is(getDefaultDataType().getMediaType().getPrimaryType()));
    assertThat(dataType.getMediaType().getSubType(), is(getDefaultDataType().getMediaType().getSubType()));
  }

  private DataType getDefaultDataType() {
    FlowRunner runner = flowRunner("defaultContentType").withPayload("");
    return runner.buildEvent().getMessage().getDataType();
  }
}
