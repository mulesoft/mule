/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import java.util.ArrayList;
import java.util.List;

public class TypeIntrospectionResult {

  private final TypeElement concreteType;
  private TypeMirror concreteTypeMirror;
  private final List<TypeIntrospectionResult> generics;

  private TypeIntrospectionResult(TypeElement concreteType, TypeMirror concreteTypeMirror,
                                  List<TypeIntrospectionResult> generics) {
    this.concreteType = concreteType;
    this.concreteTypeMirror = concreteTypeMirror;
    this.generics = generics;
  }

  public static Builder builder() {
    return new Builder();
  }

  public TypeElement getConcreteType() {
    return concreteType;
  }

  public TypeMirror getConcreteTypeMirror() {
    return concreteTypeMirror;
  }

  public List<TypeIntrospectionResult> getGenerics() {
    return generics;
  }

  public static class Builder {

    private TypeElement concreteTypeElement;
    private TypeMirror concreteTypeMirror;
    List<TypeIntrospectionResult> generics = new ArrayList<>();

    Builder setType(TypeElement concreteType, TypeMirror typeMirror) {
      this.concreteTypeMirror = typeMirror;
      this.concreteTypeElement = concreteType;
      return this;
    }

    void addGenericType(TypeIntrospectionResult result) {
      this.generics.add(result);
    }

    public TypeIntrospectionResult build() {
      return new TypeIntrospectionResult(concreteTypeElement, concreteTypeMirror, generics);
    }
  }
}
