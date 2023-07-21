/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.DispatchingContext;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;

/**
 * Default {@link DispatchingContext} implementation
 *
 * @since 4.1
 */
public class DefaultDispatchingContext implements DispatchingContext {

  private final ExtensionsClient client;

  DefaultDispatchingContext(ExtensionsClient client) {
    this.client = client;
  }

  @Override
  public ExtensionsClient getExtensionsClient() {
    return client;
  }
}
