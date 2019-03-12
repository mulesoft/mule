/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.client.DefaultExtensionsClient;

import java.util.function.Supplier;

/**
 * An argument resolver that yields instances of {@link ExtensionsClient}.
 *
 * @since 4.0
 */
public class ExtensionsClientArgumentResolver implements ArgumentResolver<ExtensionsClient> {

  private final Registry registry;
  private final PolicyManager policyManager;

  public ExtensionsClientArgumentResolver(Registry registry, PolicyManager policyManager) {
    this.registry = registry;
    this.policyManager = policyManager;
  }

  @Override
  public Supplier<ExtensionsClient> resolve(ExecutionContext executionContext) {
    return () -> {
      ExecutionContextAdapter cxt = (ExecutionContextAdapter) executionContext;
      DefaultExtensionsClient extensionClient =
          new DefaultExtensionsClient(cxt.getMuleContext(), cxt.getEvent(), registry, policyManager);
      try {
        extensionClient.initialise();
      } catch (InitialisationException e) {
        throw new MuleRuntimeException(createStaticMessage("Failed to initialise Extension Client"), e);
      }
      return extensionClient;
    };
  }
}
