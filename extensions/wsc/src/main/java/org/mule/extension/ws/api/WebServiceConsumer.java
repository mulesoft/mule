/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;

@Operations(ConsumeOperation.class)
@ConnectionProviders(WscConnectionProvider.class)
@Extension(name = "wsc")
public class WebServiceConsumer {

  @Parameter
  @Optional(defaultValue = "false")
  private boolean mtomEnabled;

  public boolean isMtomEnabled() {
    return mtomEnabled;
  }
}
