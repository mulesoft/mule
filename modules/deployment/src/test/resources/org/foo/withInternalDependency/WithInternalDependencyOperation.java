/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.withInternalDependency;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.foo.withInternalDependency.internal.InternalRegistryBean;

import javax.inject.Inject;

public class WithInternalDependencyOperation {

  @Inject
  private InternalRegistryBean registryBean;

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String injectionCheck(@Config WithInternalDependencyExtension config) throws Exception {
    config.checkExtensionInjection();
    if (registryBean == null) {
      throw new NullPointerException("registryBean is null (has not been injected into the extension operations)");
    }
    return "registryBean has been injected into the extension, its operations and it's EL functions";
  }

  public void checkConfigResolution(@Config WithInternalDependencyExtension config) {
    if (config.getDummyParameter().equals("originalValue")) {
      throw new IllegalStateException("Extension explicit config did not modify the default configuration");
    }
  }

}
