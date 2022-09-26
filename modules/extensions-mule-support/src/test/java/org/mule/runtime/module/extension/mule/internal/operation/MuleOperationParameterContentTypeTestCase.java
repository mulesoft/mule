/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationParameterContentTypeTestCase extends MuleArtifactFunctionalTestCase {

  private final String TEST_PAYLOAD = "{\"hello!\": 5}";

  @Override
  protected String getConfigFile() {
    return "mule-schema-extracting-config.xml";
  }

  @Test
  @Issue("W-11389978")
  public void whenParameterMediaTypeIsSpecifiedThenItIsPreserved() throws Exception {
    CoreEvent resultEvent = flowRunner("showSchemaFlow")
        .withMediaType(APPLICATION_JSON.withCharset(UTF_8))
        .withPayload(TEST_PAYLOAD)
        .run();

    Map<String, Object> payload = (Map<String, Object>) resultEvent.getMessage().getPayload().getValue();
    assertThat(payload.get("encoding"), is("UTF-8"));
    assertThat(payload.get("mediaType"), is("application/json; charset=UTF-8"));
    assertThat(payload.get("mimeType"), is("application/json"));
    assertThat(payload.get("raw"), is(TEST_PAYLOAD));
    assertThat(payload.get("contentLength"), is(13));

    assertThat(resultEvent.getMessage().getAttributes().getValue(), is(nullValue()));
  }

  @Test
  public void whenParameterMediaTypeIsSpecifiedThenItIsPreservedThroughOutput() throws Exception {
    CoreEvent resultEvent = flowRunner("identityFlow")
        .withMediaType(APPLICATION_JSON.withCharset(UTF_8))
        .withPayload(TEST_PAYLOAD)
        .run();

    TypedValue<String> payload = resultEvent.getMessage().getPayload();

    assertThat(payload.getValue(), is(TEST_PAYLOAD));
    assertThat(payload.getDataType(), is(like(String.class, APPLICATION_JSON, UTF_8)));
  }
}
