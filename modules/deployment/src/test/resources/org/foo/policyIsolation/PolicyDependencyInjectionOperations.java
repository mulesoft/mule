/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.policyIsolation;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.foo.policyIsolation.internal.InternalRegistryBean;

import javax.inject.Inject;

public class PolicyDependencyInjectionOperations {

  @Inject
  private InternalRegistryBean registryBean;

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String injectionCheck(@Config PolicyDependencyInjectionExtension config) throws Exception {
    config.checkExtensionInjection();
    if (registryBean == null) {
      throw new NullPointerException("registryBean is null (has not been injected into the extension operations)");
    }
    return "registryBean has been injected into the extension, its operations and it's functions";
  }

}
