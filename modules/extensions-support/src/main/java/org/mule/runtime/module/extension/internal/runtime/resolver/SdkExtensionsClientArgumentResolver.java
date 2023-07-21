/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.client.adapter.SdkExtensionsClientAdapter;

/**
 * {@link ArgumentResolver} which resolves to a {@link ExtensionsClient} by delegating into a
 * {@link ExtensionsClientArgumentResolver} and adapting the result.
 *
 * @since 4.5.0
 */
public class SdkExtensionsClientArgumentResolver implements ArgumentResolver<org.mule.sdk.api.client.ExtensionsClient> {

  private final ExtensionsClientArgumentResolver delegate;

  public SdkExtensionsClientArgumentResolver(ExtensionsClient extensionsClient) {
    delegate = new ExtensionsClientArgumentResolver(extensionsClient);
  }

  @Override
  public org.mule.sdk.api.client.ExtensionsClient resolve(ExecutionContext executionContext) {
    return new SdkExtensionsClientAdapter(delegate.resolve(executionContext));
  }
}
