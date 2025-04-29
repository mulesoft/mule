/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.test;

import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.SetPayloadStory.SET_PAYLOAD;

import static java.nio.charset.StandardCharsets.UTF_16;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.simple.SetPayloadMessageProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CORE_COMPONENTS)
@Story(SET_PAYLOAD)
public class SetPayloadMessageProcessorTestCase extends AbstractMuleTestCase {

  private static final String PLAIN_TEXT = "This is a plain text";
  private static final String EXPRESSION = "#[vars.testVariable]";
  private static final Charset CUSTOM_ENCODING = UTF_16;

  private SetPayloadMessageProcessor setPayloadMessageProcessor;

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  @Before
  public void setUp() throws Exception {
    setPayloadMessageProcessor = new SetPayloadMessageProcessor();
    setPayloadMessageProcessor.setExpressionManager(dw.getExpressionManager());
  }

  @Test
  public void setsNullPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(null);
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void setsPlainText() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());
    assertThat(response.getMessage().getPayload().getValue(), is(PLAIN_TEXT));
  }

  @Test
  public void setsExpressionPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(EXPRESSION);
    setPayloadMessageProcessor.initialise();


    CoreEvent response = setPayloadMessageProcessor.process(CoreEvent.builder(testEvent())
        .addVariable("testVariable", PLAIN_TEXT, STRING)
        .build());

    assertThat(response.getMessage().getPayload().getValue(), is(PLAIN_TEXT));
  }

  @Test
  public void setsDefaultDataTypeForNullPayload() throws MuleException {
    setPayloadMessageProcessor.setValue(null);
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getDataType(), like(Object.class, MediaType.ANY, null));
  }

  @Test
  public void setsDefaultDataTypeForNonNullValue() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.initialise();

    setPayloadMessageProcessor.process(testEvent());

    assertThat(testEvent().getMessage().getPayload().getDataType(), like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setsCustomEncoding() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.setDataType(DataType.builder().charset(CUSTOM_ENCODING).build());
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getDataType(), like(String.class, MediaType.ANY, CUSTOM_ENCODING));
  }

  @Test
  public void setsCustomMimeType() throws MuleException {
    setPayloadMessageProcessor.setValue(PLAIN_TEXT);
    setPayloadMessageProcessor.setDataType(DataType.builder().mediaType(MediaType.APPLICATION_XML).build());
    setPayloadMessageProcessor.initialise();

    CoreEvent response = setPayloadMessageProcessor.process(testEvent());

    assertThat(response.getMessage().getPayload().getDataType(), like(String.class, MediaType.APPLICATION_XML, null));
  }
}
