/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import java.util.List;

/**
 * Describes de generics of a Type
 *
 * @since 4.1
 */
public class TypeGeneric {

  private final Type concreteType;
  private final List<TypeGeneric> generics;

  public TypeGeneric(Type concreteType, List<TypeGeneric> generics) {
    this.concreteType = concreteType;
    this.generics = generics;
  }

  public Type getConcreteType() {
    return concreteType;
  }

  public List<TypeGeneric> getGenerics() {
    return generics;
  }
}
