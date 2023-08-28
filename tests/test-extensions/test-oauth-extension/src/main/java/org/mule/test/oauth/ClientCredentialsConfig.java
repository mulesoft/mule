/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
