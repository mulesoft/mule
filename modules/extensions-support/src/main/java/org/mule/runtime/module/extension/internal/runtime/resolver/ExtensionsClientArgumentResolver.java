/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.client.DefaultExtensionsClient;
import org.mule.runtime.module.extension.internal.runtime.client.strategy.ExtensionsClientProcessorsStrategyFactory;

import java.util.function.Supplier;

/**
 * An argument resolver that yields instances of {@link ExtensionsClient}.
 *
 * @since 4.0
 */
public class ExtensionsClientArgumentResolver implements ArgumentResolver<ExtensionsClient> {

  private final ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory;

  public ExtensionsClientArgumentResolver(ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory) {
    this.extensionsClientProcessorsStrategyFactory = extensionsClientProcessorsStrategyFactory;
  }

  @Override
  public Supplier<ExtensionsClient> resolve(ExecutionContext executionContext) {
    return () -> {
      ExecutionContextAdapter cxt = (ExecutionContextAdapter) executionContext;
      DefaultExtensionsClient extensionClient =
          new DefaultExtensionsClient(cxt.getEvent(), extensionsClientProcessorsStrategyFactory);
      try {
        extensionClient.initialise();
      } catch (InitialisationException e) {
        throw new MuleRuntimeException(createStaticMessage("Failed to initialise Extension Client"), e);
      }
      return extensionClient;
    };
  }
}
