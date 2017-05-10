/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
