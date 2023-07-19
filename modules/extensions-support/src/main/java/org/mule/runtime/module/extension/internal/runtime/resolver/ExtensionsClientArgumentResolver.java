/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.client.EventedExtensionsClientDecorator;

/**
 * An argument resolver that yields instances of {@link ExtensionsClient}.
 *
 * @since 4.0
 */
public class ExtensionsClientArgumentResolver implements ArgumentResolver<ExtensionsClient> {

  private final ExtensionsClient extensionsClient;

  public ExtensionsClientArgumentResolver(ExtensionsClient extensionsClient) {
    this.extensionsClient = extensionsClient;
  }

  @Override
  public ExtensionsClient resolve(ExecutionContext executionContext) {
    return new EventedExtensionsClientDecorator(extensionsClient, ((ExecutionContextAdapter) executionContext).getEvent());
  }
}
