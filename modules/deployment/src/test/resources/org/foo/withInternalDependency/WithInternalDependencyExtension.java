/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.withInternalDependency;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.foo.withInternalDependency.internal.InternalRegistryBean;

import javax.inject.Inject;

/**
 * Extension for testing purposes that declares an internal bean dependency.
 */
@Extension(name = "WithInternalDependency")
@Operations({WithInternalDependencyOperation.class})
public class WithInternalDependencyExtension {

  @Inject
  private InternalRegistryBean registryBean;

  public WithInternalDependencyExtension() {}

  public String checkDependency() {
    if (registryBean == null) {
      throw new NullPointerException("registryBean is null (has not been injected)");
    }
    return "registryBean has been injected";
  }
}
