/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.Content;

import java.io.InputStream;

public class InputStreamParameterGroup {

  @Parameter
  @Content(primary = true)
  private InputStream inputStreamContent;

  public InputStream getInputStreamContent() {
    return inputStreamContent;
  }
}
