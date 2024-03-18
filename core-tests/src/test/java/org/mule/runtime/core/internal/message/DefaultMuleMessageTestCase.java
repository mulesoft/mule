/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DefaultMuleMessageTestCase extends AbstractMuleContextTestCase {

  @Test
  public void regularToString() {
    Message message = Message.builder()
        .payload(TypedValue.of("test"))
        .attributes(new TypedValue<>("{}", JSON_STRING))
        .mediaType(TEXT)
        .build();

    assertThat(message.toString(), is(equalToIgnoringLineBreaks("\n" +
        "org.mule.runtime.core.internal.message.DefaultMessageBuilder$MessageImplementation\n"
        + "{\n"
        + "  payload=test\n"
        + "  mediaType=text/plain\n"
        + "  attributes={}\n"
        + "  attributesMediaType=application/json\n"
        + "}")));
  }

}
