/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import org.mule.metadata.api.builder.TypeBuilder;
import org.mule.metadata.ast.api.TypeHandler;
import org.mule.metadata.ast.internal.IntrospectionContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.Types;

import java.util.List;

/**
 * {@link TypeHandler} implementation for Extension Types.
 *
 * @since 4.1
 */
public class ExtensionTypeHandler implements TypeHandler {

  private ProcessingEnvironment processingEnvironment;

  public ExtensionTypeHandler(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  @Override
  public boolean handles(TypeMirror typeMirror) {
    return typeMirror instanceof DeclaredType
        && isHandled(typeMirror);
  }

  private boolean isHandled(TypeMirror typeMirror) {
    return isSameType(typeMirror, TypedValue.class)
        || isSameType(typeMirror, ParameterResolver.class)
        || isSameType(typeMirror, Literal.class);
  }

  @Override
  public TypeBuilder<?> handle(TypeMirror typeMirror, TypeVisitor<TypeBuilder<?>, IntrospectionContext> typeVisitor,
                               IntrospectionContext context) {
    if (typeMirror instanceof DeclaredType && isHandled(typeMirror)) {
      List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
      if (typeArguments.isEmpty()) {
        throw new IllegalArgumentException("The given TypeMirror " + typeMirror + " doesn't provide generics");
      }
      return typeArguments.get(0).accept(typeVisitor, context);
    }
    throw new IllegalArgumentException("The given TypeMirror " + typeMirror + " it's not supported by this Handler");
  }

  public boolean isSameType(TypeMirror typeMirror, Class aClass) {
    Types types = processingEnvironment.getTypeUtils();
    TypeMirror erasure = types.erasure(typeMirror);
    TypeMirror clazz = types.erasure(processingEnvironment.getElementUtils().getTypeElement(aClass.getName()).asType());

    return types.isSameType(erasure, clazz);
  }
}
