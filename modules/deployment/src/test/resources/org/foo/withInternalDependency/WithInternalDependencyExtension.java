/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.withInternalDependency;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.foo.withInternalDependency.internal.InternalRegistryBean;
import org.foo.withInternalDependency.internal.WithInternalDependencyFunctions;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.lang.String;
import javax.inject.Inject;

/**
 * Extension for testing purposes that declares an internal bean dependency.
 */
@Extension(name = "WithInternalDependency")
@ExpressionFunctions(WithInternalDependencyFunctions.class)
@Operations({WithInternalDependencyOperation.class})
public class WithInternalDependencyExtension {

//  @Parameter
//  @Expression
//  boolean isInternalDependencyInjectedIntoFunctions = false;

  @Inject
  private InternalRegistryBean registryBean;

  public void checkExtensionInjection() {
//    if (!isInternalDependencyInjectedIntoFunctions) {
//      throw new NullPointerException("registryBean is null (has not been injected into the extension expression functions)");
//    }
    if (registryBean == null) {
      throw new NullPointerException("registryBean is null (has not been injected into the extension)");
    }
  }
}
