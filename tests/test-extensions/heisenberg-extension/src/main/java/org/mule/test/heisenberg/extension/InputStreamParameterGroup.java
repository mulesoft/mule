/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
