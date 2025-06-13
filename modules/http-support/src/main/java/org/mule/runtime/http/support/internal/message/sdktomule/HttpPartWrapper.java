/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.sdktomule;

import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;
import org.mule.sdk.api.http.domain.entity.multipart.Part;

import java.io.IOException;
import java.io.InputStream;

public class HttpPartWrapper extends HttpPart {

  public HttpPartWrapper(Part sdkPart) {
    super(sdkPart.getName(), sdkPart.getFileName(), consumeStream(sdkPart), sdkPart.getContentType(), (int) sdkPart.getSize());
  }

  private static byte[] consumeStream(Part sdkPart) {
    try {
      InputStream is = sdkPart.getInputStream();
      return is.readAllBytes();
    } catch (IOException ioe) {
      return new byte[0];
    }
  }
}
