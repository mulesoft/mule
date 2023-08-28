/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.typed.value;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.nio.charset.Charset;

public abstract class AbstractTypedValueTestCase extends AbstractExtensionFunctionalTestCase {

  static final Charset UTF8 = Charset.forName("UTF-8");
  static final MediaType APPLICATION_JAVA = MediaType.parse("application/java");
  static final MediaType WILDCARD = MediaType.parse("*/*");
  static final KnockeableDoor DOOR = new KnockeableDoor("Saul");

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  void runAndAssertTypedValue(String flowName, Object payloadValue, MediaType mediaType, Charset charset)
      throws Exception {
    Object payload = flowRunner(flowName).run().getMessage().getPayload().getValue();
    assertTypedValue((TypedValue) payload, payloadValue, mediaType, charset);
  }

  void assertTypedValue(TypedValue typedValue, Object payloadValue, MediaType mediaType, Charset charset) {
    assertThat(typedValue, is(instanceOf(TypedValue.class)));
    Object value = typedValue.getValue();
    assertThat(value, is(payloadValue));
    if (value != null) {
      assertThat(value, is(instanceOf(payloadValue.getClass())));
      assertThat(typedValue.getDataType(), is(like(payloadValue.getClass(), mediaType, charset)));
    }
  }
}
