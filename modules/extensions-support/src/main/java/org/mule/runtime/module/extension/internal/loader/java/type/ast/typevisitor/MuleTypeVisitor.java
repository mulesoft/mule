/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor;

import org.mule.runtime.module.extension.internal.loader.java.type.ast.typevisitor.TypeIntrospectionResult.Builder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

public class MuleTypeVisitor
    implements TypeVisitor<TypeIntrospectionResult, Builder> {

  private ProcessingEnvironment processingEnvironment;

  public MuleTypeVisitor(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  @Override
  public TypeIntrospectionResult visit(TypeMirror t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visit(TypeMirror t) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitPrimitive(PrimitiveType t, Builder resultBuilder) {
    return resultBuilder.setType(null, t).build();
  }

  @Override
  public TypeIntrospectionResult visitNull(NullType t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitArray(ArrayType t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitDeclared(DeclaredType t, Builder resultBuilder) {
    resultBuilder.setType((TypeElement) t.asElement(), t);

    for (TypeMirror typeMirror : t.getTypeArguments()) {
      TypeIntrospectionResult accept = typeMirror.accept(this, TypeIntrospectionResult.builder());
      resultBuilder.addGenericType(accept);
    }

    return resultBuilder.build();
  }

  @Override
  public TypeIntrospectionResult visitError(ErrorType t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitTypeVariable(TypeVariable t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitWildcard(WildcardType t, Builder resultBuilder) {
    return resultBuilder.setType(processingEnvironment.getElementUtils().getTypeElement(Object.class.getTypeName()), t).build();
  }

  @Override
  public TypeIntrospectionResult visitExecutable(ExecutableType t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitNoType(NoType t, Builder resultBuilder) {
    resultBuilder.setType(processingEnvironment.getElementUtils().getTypeElement(Void.class.getTypeName()), t);
    return resultBuilder.build();
  }

  @Override
  public TypeIntrospectionResult visitUnknown(TypeMirror t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitUnion(UnionType t, Builder resultBuilder) {
    return null;
  }

  @Override
  public TypeIntrospectionResult visitIntersection(IntersectionType t, Builder resultBuilder) {
    return null;
  }
}
