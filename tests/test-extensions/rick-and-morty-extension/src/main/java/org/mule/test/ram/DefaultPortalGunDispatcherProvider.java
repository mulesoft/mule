/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.ram;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class DefaultPortalGunDispatcherProvider extends AbstractScienceTransportProvider {

  @Parameter
  private String responseMessage;

  @Override
  protected String getResponseWord() {
    return responseMessage;
  }
}
