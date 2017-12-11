/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import java.util.List;

/**
 * Describes de generics of a Type
 *
 * @since 4.1
 */
public class GenericInfo {

  private final Type concreteType;
  private final List<GenericInfo> generics;

  public GenericInfo(Type concreteType, List<GenericInfo> generics) {
    this.concreteType = concreteType;
    this.generics = generics;
  }

  public Type getConcreteType() {
    return concreteType;
  }

  public List<GenericInfo> getGenerics() {
    return generics;
  }
}
