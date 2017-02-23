/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.ExplicitMethodEntryPointResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ObjectFactory} for explicit method selection for {@link org.mule.runtime.core.api.component.Component} configuration.
 *
 * @since 4.0
 */
public class ExplicitMethodEntryPointResolverObjectFactory extends AbstractAnnotatedObjectFactory<EntryPointResolver> {

  private boolean acceptVoidMethods;
  private List<MethodEntryPoint> methodEntryPoints = new ArrayList<>();

  /**
   * @param acceptVoidMethods true if void methods may be invoked, false otherwise.
   */
  public void setAcceptVoidMethods(boolean acceptVoidMethods) {
    this.acceptVoidMethods = acceptVoidMethods;
  }

  /**
   * @param methodEntryPoints list of methods that may be resolved for execution.
   */
  public void setMethodEntryPoints(List<MethodEntryPoint> methodEntryPoints) {
    this.methodEntryPoints = methodEntryPoints;
  }

  @Override
  public EntryPointResolver doGetObject() throws Exception {
    ExplicitMethodEntryPointResolver explicitMethodEntryPointResolver = new ExplicitMethodEntryPointResolver();
    explicitMethodEntryPointResolver.setAcceptVoidMethods(acceptVoidMethods);
    for (MethodEntryPoint methodEntryPoint : methodEntryPoints) {
      explicitMethodEntryPointResolver.addMethod(methodEntryPoint.getMethod());
    }
    return explicitMethodEntryPointResolver;
  }
}
