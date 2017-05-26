/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import org.mule.runtime.extension.api.runtime.source.Source;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * A contract for an element from which a message source can be derived
 *
 * @since 4.0
 */
public interface SourceElement extends ParameterizableTypeElement {

  /**
   * @return The list of generics of the super class {@link Source}
   */
  List<Type> getSuperClassGenerics();

  @Override
  Class<? extends Source> getDeclaringClass();

  //TODO: MULE-9220 not more than one
  Optional<MethodElement> getOnResponseMethod();

  //TODO: MULE-9220 not more than one
  Optional<MethodElement> getOnErrorMethod();

  Optional<MethodElement> getOnTerminateMethod();

}
