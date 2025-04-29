/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.transformer.simple;

import static org.mule.runtime.api.config.MuleRuntimeFeature.SET_VARIABLE_WITH_NULL_VALUE;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Field;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import jakarta.activation.MimeTypeParseException;

@SmallTest
public abstract class AbstractAddVariablePropertyProcessorTestCase extends AbstractMuleTestCase {

  public static final Charset ENCODING = US_ASCII;
  public static final String PLAIN_STRING_KEY = "someText";
  public static final String PLAIN_STRING_VALUE = "someValue";
  public static final String EXPRESSION = "#['someValue']";
  public static final String NULL_EXPRESSION = "#[null]";
  public static final Charset CUSTOM_ENCODING = UTF_8;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  @Mock(lenient = true)
  private StreamingManager streamingManager;

  private CoreEvent event;
  private Message message;
  private final AbstractAddVariablePropertyProcessor addVariableProcessor;
  private CheckedRunnable afterAssertions;

  public AbstractAddVariablePropertyProcessorTestCase(AbstractAddVariablePropertyProcessor abstractAddVariableProcessor) {
    addVariableProcessor = abstractAddVariableProcessor;
  }

  @Before
  public void setUpTest() throws Exception {
    when(streamingManager.manage(any(CursorProvider.class), any(EventContext.class))).thenAnswer(inv -> inv.getArgument(0));

    addVariableProcessor.setExpressionManager(dw.getExpressionManager());
    addVariableProcessor.setStreamingManager(streamingManager);
    addVariableProcessor.setArtifactEncoding(Charset::defaultCharset);

    message = of("");
    event = createTestEvent(message);

    afterAssertions = () -> verify(streamingManager, never()).manage(any(CursorProvider.class), any(EventContext.class));

    FeatureFlaggingService featureFlaggingService = mock(FeatureFlaggingService.class);
    Field f1 = addVariableProcessor.getClass().getSuperclass().getDeclaredField("featureFlaggingService");
    f1.setAccessible(true);
    f1.set(addVariableProcessor, featureFlaggingService);
    when(featureFlaggingService.isEnabled(SET_VARIABLE_WITH_NULL_VALUE)).thenReturn(true);
  }

  @After
  public void after() {
    if (afterAssertions != null) {
      afterAssertions.run();
    }
  }

  protected CoreEvent createTestEvent(Message message) throws MuleException {
    return eventBuilder().message(message).build();
  }

  @Test
  public void testAddVariable() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY), like(String.class, ANY, defaultCharset()));
  }

  @Test
  public void testAddVariableWithExpressionValue() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(EXPRESSION);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, APPLICATION_JAVA, defaultCharset()));
  }

  @Test
  public void testAddVariableWithExpressionKey() throws MuleException {
    addVariableProcessor.setIdentifier(EXPRESSION);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_VALUE, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_VALUE), like(String.class, ANY, defaultCharset()));
  }

  @Test
  public void testAddVariableWithEncoding() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    addVariableProcessor.setReturnDataType(DataType.builder().charset(CUSTOM_ENCODING).build());
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY), like(String.class, ANY, CUSTOM_ENCODING));
  }

  @Test
  public void testAddVariableWithMimeType() throws MimeTypeParseException, MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    addVariableProcessor.setReturnDataType(DataType.builder().mediaType(APPLICATION_XML).build());
    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
    assertThat(getVariableDataType(event, PLAIN_STRING_KEY),
               like(String.class, APPLICATION_XML, defaultCharset()));
  }

  protected abstract DataType getVariableDataType(CoreEvent event, String key);

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithNullKey() {
    addVariableProcessor.setIdentifier(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddVariableWithEmptyKey() {
    addVariableProcessor.setIdentifier("");
  }

  @Test(expected = NullPointerException.class)
  public void testAddVariableWithNullValue() {
    addVariableProcessor.setValue(null);
  }

  @Test
  public void testAddVariableWithNullExpressionKeyResult() throws MuleException {
    addVariableProcessor.setIdentifier(NULL_EXPRESSION);
    addVariableProcessor.setValue(PLAIN_STRING_VALUE);
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(event);
    verifyNotAdded(event);
  }

  @Test
  public void testAddVariableWithNullExpressionValueResult() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(NULL_EXPRESSION);
    addVariableProcessor.initialise();
    verifyRemoved(event, PLAIN_STRING_KEY);
  }

  @Test
  public void testAddVariableWithNullPayloadExpressionValueResult() throws MuleException {
    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue(NULL_EXPRESSION);
    addVariableProcessor.initialise();

    event = addVariableProcessor.process(event);

    verifyAdded(event, PLAIN_STRING_KEY, null);
  }

  @Test
  public void testCursorProvidersAreManaged() throws MuleException {
    CursorProvider cursorProvider = mock(CursorProvider.class);

    addVariableProcessor.setIdentifier(PLAIN_STRING_KEY);
    addVariableProcessor.setValue("#[payload]");
    addVariableProcessor.initialise();
    event = addVariableProcessor.process(CoreEvent.builder(event).message(of(cursorProvider)).build());

    assertThat(event.getVariables().get(PLAIN_STRING_KEY).getValue(), is(cursorProvider));
    verify(streamingManager).manage(same(cursorProvider), any(EventContext.class));

    afterAssertions = null;
  }

  protected abstract void verifyAdded(CoreEvent event, String key, String value);

  protected abstract void verifyNotAdded(CoreEvent mockEvent);

  protected abstract void verifyRemoved(CoreEvent mockEvent, String key);

}
