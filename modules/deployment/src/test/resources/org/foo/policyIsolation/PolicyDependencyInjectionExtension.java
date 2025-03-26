/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.policyIsolation;

import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.foo.policyIsolation.internal.InternalRegistryBean;
import org.foo.policyIsolation.internal.PolicyDependencyInjectionFunctions;

import jakarta.inject.Inject;

/**
 * Extension designed to test the policy classloading and context isolation
 * by injecting an internal dependency (see registry-bootstrap.properties).
 */
@Extension(name = "policyDependencyInjection")
@ExpressionFunctions(PolicyDependencyInjectionFunctions.class)
@Operations({PolicyDependencyInjectionOperations.class})
public class PolicyDependencyInjectionExtension {

  @Inject
  private InternalRegistryBean registryBean;

  public void checkExtensionInjection() {
    if (registryBean == null) {
      throw new NullPointerException("registryBean is null (has not been injected into the extension)");
    }
  }

}
