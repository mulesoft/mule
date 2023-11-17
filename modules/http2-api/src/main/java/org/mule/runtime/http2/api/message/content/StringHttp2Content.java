/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message.content;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;

public class StringHttp2Content extends BaseHttp2Content {

  private final String asString;

  public StringHttp2Content(String asString) {
    super(new ByteArrayInputStream(asString.getBytes(UTF_8)));
    this.asString = asString;
  }

  @Override
  public boolean isEmpty() {
    return asString.isEmpty();
  }
}
