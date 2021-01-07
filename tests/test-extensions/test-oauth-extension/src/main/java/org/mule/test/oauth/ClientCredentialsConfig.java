/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

@ConnectionProviders(TestOAuthClientCredentialsProvider.class)
@Operations(TestOAuthOperations.class)
@Configuration(name = "client-credentials")
public class ClientCredentialsConfig implements Disposable {

  private int dispose = 0;

  @Override
  public void dispose() {
    dispose++;
  }

  public int getDispose() {
    return dispose;
  }
}
