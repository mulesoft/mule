/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static java.nio.charset.Charset.availableCharsets;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ContentTypeHandlingTestCase extends AbstractExtensionFunctionalTestCase {

  private static Charset customEncoding;
  private static final long PROBER_TIMEOUT = 15000;
  private static final long PROBER_FREQUENCY = 1000;

  @Rule
  public SystemProperty customEncodingProperty = new SystemProperty("customEncoding", customEncoding.name());

  @Override
  protected String getConfigFile() {
    return "content-type-handling-config.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @BeforeClass
  public static void before() throws Exception {
    customEncoding = defaultCharset().equals(UTF_8) ? UTF_8 : ISO_8859_1;
  }

  @Before
  public void clearMediaTypes() {
    MediaTypeCollectorProcessor.clearMediaTypes();
  }

  @Test
  public void setsContentTypeOnXml() throws Exception {
    CoreEvent response = runFlow("setsContentTypeOnXml");
    DataType dataType = response.getMessage().getPayload().getDataType();
    assertCustomEncoding(dataType);
    assertThat(dataType.getMediaType().getPrimaryType(), is(MediaType.TEXT.getPrimaryType()));
    assertThat(dataType.getMediaType().getSubType(), is(MediaType.TEXT.getSubType()));
  }

  @Test
  public void onlySetEncodingOnXml() throws Exception {
    CoreEvent response = runFlow("onlySetEncodingOnXml");
    DataType dataType = response.getMessage().getPayload().getDataType();
    assertCustomEncoding(dataType);
  }

  @Test
  public void onlySetMimeTypeOnXml() throws Exception {
    CoreEvent response = runFlow("onlySetMimeTypeOnXml");
    DataType dataType = response.getMessage().getPayload().getDataType();
    assertDefaultEncoding(dataType);
    assertCustomMimeType(dataType);
  }

  @Test
  public void maintainsContentType() throws Exception {
    CoreEvent response = flowRunner("defaultContentType").withPayload("").run();
    final DataType responseDataType = response.getMessage().getPayload().getDataType();
    assertDefaultEncoding(responseDataType);
    assertDefaultMimeType(responseDataType);
  }

  @Test
  public void setEncodingInMimeTypeAndParam() throws Exception {
    CoreEvent response = runFlow("setEncodingInMimeTypeAndParam");
    DataType dataType = response.getMessage().getPayload().getDataType();
    assertThat(dataType.getMediaType().getPrimaryType(), is("application"));
    assertThat(dataType.getMediaType().getSubType(), is("json"));
    assertThat(dataType.getMediaType().getCharset().get(), is(StandardCharsets.UTF_16));
  }

  @Test
  public void overridesContentType() throws Exception {
    Charset lastSupportedEncoding = availableCharsets().values().stream().reduce((first, last) -> last).get();
    CoreEvent response = runFlow("setsContentTypeProgrammatically");

    final DataType dataType = response.getMessage().getPayload().getDataType();
    assertCustomMimeType(dataType);
    assertThat(dataType.getMediaType().getCharset().get(), is(lastSupportedEncoding));
  }

  @Test
  public void sourceOverridesContentType() throws Exception {
    startFlow("sourceMimeType");
    check(PROBER_TIMEOUT, PROBER_FREQUENCY, () -> {
      if (MediaTypeCollectorProcessor.getMediaTypes().size() == 2) {
        return true;
      }
      return false;
    });
    assertThat(MediaTypeCollectorProcessor.getMediaTypes().get(0).getCharset().get().displayName(), is("UTF-16"));
    assertThat(MediaTypeCollectorProcessor.getMediaTypes().get(0).getPrimaryType(), is("pet"));
    assertThat(MediaTypeCollectorProcessor.getMediaTypes().get(0).getSubType(), is("plain"));
    assertThat(MediaTypeCollectorProcessor.getMediaTypes().get(0).getParameter("header"), is("false"));
    assertThat(MediaTypeCollectorProcessor.getMediaTypes().get(1).getPrimaryType(), is("dead"));
    assertThat(MediaTypeCollectorProcessor.getMediaTypes().get(1).getSubType(), is("json"));
  }

  @Test
  public void sourceWithAListAsResultOverridesContentType() throws Exception {
    startFlow("sourceListMimeType");
    check(PROBER_TIMEOUT, PROBER_FREQUENCY, () -> MediaTypeCollectorProcessor.getMediaTypes().size() == 3);

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getPrimaryType().equals("pet")));

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getSubType().equals("plain")));
  }

  @Test
  public void operationWithListResultStringOutputOverridesContentType() throws Exception {
    runFlow("listStringMediaType");

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getPrimaryType().equals("application")));

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getSubType().equals("java")));
  }

  @Test
  public void operationWithListResultStreamOutputOverridesContentType() throws Exception {
    runFlow("listStreamMediaType");
    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getCharset().get().displayName().equals("UTF-16")));

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getPrimaryType().equals("application")));

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getSubType().equals("java")));
  }

  @Test
  public void operationWithPagingProviderResultCursorProviderOutputOverridesContentType() throws Exception {
    runFlow("pagedCursorProviderMediaType");
    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getCharset().get().displayName().equals("UTF-16")));

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getPrimaryType().equals("application")));

    assertTrue(MediaTypeCollectorProcessor.getMediaTypes().stream()
        .allMatch(mediaType -> mediaType.getSubType().equals("java")));
  }

  @Test
  public void strictMimeType() throws Exception {
    CoreEvent response = runFlow("strictMimeType");
    assertThat(response.getMessage().getPayload().getDataType().getMediaType().matches(TEXT), is(true));
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
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
    return runner.buildEvent().getMessage().getPayload().getDataType();
  }

  private static class MediaTypeCollectorProcessor extends AbstractComponent implements Processor {

    private static List<MediaType> MEDIA_TYPES = new LinkedList<>();

    public static List<MediaType> getMediaTypes() {
      return MEDIA_TYPES;
    }

    public static void clearMediaTypes() {
      MEDIA_TYPES = new LinkedList<>();
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      MEDIA_TYPES.add(event.getMessage().getPayload().getDataType().getMediaType());
      return event;
    }
  }
}
