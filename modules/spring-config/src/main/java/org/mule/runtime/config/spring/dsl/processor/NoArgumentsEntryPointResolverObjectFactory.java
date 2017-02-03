/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.spring.ExcludeDefaultObjectMethods;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ObjectFactory} for <no-arguments-entry-point-resolver> configuration element which defines that a method with no
 * arguments should be invoked.
 *
 * @since 4.0
 */
public class NoArgumentsEntryPointResolverObjectFactory extends AbstractAnnotatedObjectFactory<EntryPointResolver> {

  private ExcludeDefaultObjectMethods excludeDefaultObjectMethods;
  private List<MethodEntryPoint> methodEntryPoints = new ArrayList<>();

  @Override
  public EntryPointResolver doGetObject() throws Exception {
    NoArgumentsEntryPointResolver noArgumentsEntryPointResolver = new NoArgumentsEntryPointResolver();
    if (excludeDefaultObjectMethods != null) {
      noArgumentsEntryPointResolver.setIgnoredMethods(excludeDefaultObjectMethods.getExcludedMethods());
    }
    for (MethodEntryPoint methodEntryPoint : methodEntryPoints) {
      if (methodEntryPoint.isEnabled()) {
        noArgumentsEntryPointResolver.addMethod(methodEntryPoint.getMethod());
      } else {
        noArgumentsEntryPointResolver.addIgnoredMethod(methodEntryPoint.getMethod());
      }
    }
    return noArgumentsEntryPointResolver;
  }

  public void setExcludeDefaultObjectMethods(ExcludeDefaultObjectMethods excludeDefaultObjectMethods) {
    this.excludeDefaultObjectMethods = excludeDefaultObjectMethods;
  }

  public void setMethodEntryPoints(List<MethodEntryPoint> methodEntryPoints) {
    this.methodEntryPoints = methodEntryPoints;
  }
}
