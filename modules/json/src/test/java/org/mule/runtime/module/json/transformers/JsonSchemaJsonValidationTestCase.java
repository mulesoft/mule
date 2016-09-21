/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.transformers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonSchemaJsonValidationTestCase extends AbstractMuleContextTestCase {

  private static final String EXPECTED_JSON = "{\n" + "  \"homeTeam\": \"BAR\",\n" + "  \"awayTeam\": \"RMA\",\n"
      + "  \"homeTeamScore\": 3,\n" + "  \"awayTeamScore\": 0\n" + "}";

  private static final String BAD_JSON = "{\n" + "  \"homeTeam\": \"BARCA\",\n" + "  \"awayTeam\": \"RMA\",\n"
      + "  \"homeTeamScore\": 3,\n" + "  \"awayTeamScore\": 0\n" + "}";

  private JsonSchemaValidationFilter filter;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    filter = new JsonSchemaValidationFilter();
    filter.setSchemaLocations("match-schema.json");
    filter.setMuleContext(muleContext);
    filter.initialise();
  }

  @Test
  public void filterShouldAcceptStringInput() throws Exception {
    Event event = eventBuilder().message(InternalMessage.of(EXPECTED_JSON)).build();
    Builder builder = Event.builder(event);
    boolean accepted = filter.accept(event, builder);
    assertTrue(accepted);
    JSONAssert.assertEquals(EXPECTED_JSON, getPayloadAsString(builder.build().getMessage()), false);
  }

  @Test
  public void filterShouldAcceptReaderInput() throws Exception {
    Event event = eventBuilder().message(InternalMessage.of(new StringReader(EXPECTED_JSON))).build();
    Builder builder = Event.builder(event);
    boolean accepted = filter.accept(event, builder);
    assertTrue(accepted);
    JSONAssert.assertEquals(EXPECTED_JSON, getPayloadAsString(builder.build().getMessage()), false);
  }

  @Test
  public void filterShouldAcceptByteArrayInput() throws Exception {
    Event event = eventBuilder().message(InternalMessage.of(EXPECTED_JSON.getBytes())).build();
    Builder builder = Event.builder(event);
    boolean accepted = filter.accept(event, builder);
    assertTrue(accepted);
    JSONAssert.assertEquals(EXPECTED_JSON, getPayloadAsString(builder.build().getMessage()), false);
  }

  @Test
  public void filterShouldAcceptInputStreamInput() throws Exception {
    Event event = eventBuilder().message(InternalMessage.of(new ByteArrayInputStream(EXPECTED_JSON.getBytes()))).build();
    Builder builder = Event.builder(event);
    boolean accepted = filter.accept(event, builder);
    assertTrue(accepted);
    JSONAssert.assertEquals(EXPECTED_JSON, getPayloadAsString(builder.build().getMessage()), false);
  }

  @Test
  public void filterShouldNotAcceptInvalidJson() throws Exception {
    Event event = eventBuilder().message(InternalMessage.of(BAD_JSON)).build();
    boolean accepted = filter.accept(event, Event.builder(event));
    assertFalse(accepted);
  }

}
