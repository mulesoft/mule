/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.Optional.ofNullable;
import static javax.lang.model.type.TypeKind.BOOLEAN;
import static javax.lang.model.type.TypeKind.BYTE;
import static javax.lang.model.type.TypeKind.CHAR;
import static javax.lang.model.type.TypeKind.DOUBLE;
import static javax.lang.model.type.TypeKind.FLOAT;
import static javax.lang.model.type.TypeKind.INT;
import static javax.lang.model.type.TypeKind.LONG;
import static javax.lang.model.type.TypeKind.SHORT;
import static javax.lang.model.type.TypeKind.VOID;

import org.mule.runtime.module.extension.internal.loader.java.type.GenericInfo;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.reflect.TypeToken;

class ASTUtils {

  private ProcessingEnvironment processingEnvironment;
  //TODO - This will be removed once there is a AST MetadataType TypeLoader
  private Map<String, Class> primitiveTypesClasses = new HashMap<>();
  private Map<String, TypeMirror> primitiveTypeMirrors = new HashMap<>();

  /**
   *
   * @param processingEnvironment
   */
  ASTUtils(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
    primitiveTypeMirrors.put("int", processingEnvironment.getTypeUtils().getPrimitiveType(INT));
    primitiveTypeMirrors.put("long", processingEnvironment.getTypeUtils().getPrimitiveType(LONG));
    primitiveTypeMirrors.put("double", processingEnvironment.getTypeUtils().getPrimitiveType(DOUBLE));
    primitiveTypeMirrors.put("float", processingEnvironment.getTypeUtils().getPrimitiveType(FLOAT));
    primitiveTypeMirrors.put("boolean", processingEnvironment.getTypeUtils().getPrimitiveType(BOOLEAN));
    primitiveTypeMirrors.put("char", processingEnvironment.getTypeUtils().getPrimitiveType(CHAR));
    primitiveTypeMirrors.put("byte", processingEnvironment.getTypeUtils().getPrimitiveType(BYTE));
    primitiveTypeMirrors.put("void", processingEnvironment.getTypeUtils().getNoType(VOID));
    primitiveTypeMirrors.put("short", processingEnvironment.getTypeUtils().getPrimitiveType(SHORT));

    primitiveTypesClasses.put("int", Integer.TYPE);
    primitiveTypesClasses.put("long", Long.TYPE);
    primitiveTypesClasses.put("double", Double.TYPE);
    primitiveTypesClasses.put("float", Float.TYPE);
    primitiveTypesClasses.put("boolean", Boolean.TYPE);
    primitiveTypesClasses.put("char", Character.TYPE);
    primitiveTypesClasses.put("byte", Byte.TYPE);
    primitiveTypesClasses.put("void", Void.TYPE);
    primitiveTypesClasses.put("short", Short.TYPE);
  }

  <T extends Annotation> ASTBasedValueFetcher<T> fromAnnotation(Class<T> annotationClass, Element element) {
    return new ASTBasedValueFetcher<>(annotationClass, element, processingEnvironment);
  }

  java.lang.reflect.Type getReflectType(ASTType type) {
    List<GenericInfo> generics = type.getGenerics();
    try {
      if (type.getTypeElement() != null) {
        String typeName = processingEnvironment.getElementUtils().getBinaryName(type.getTypeElement()).toString();
        if (!generics.isEmpty()) {
          java.lang.reflect.Type[] types = generics.stream().map(ASTUtils::toType).toArray(java.lang.reflect.Type[]::new);
          return TypeToken.getParameterized(Class.forName(typeName), types).getType();
        } else {
          return Class.forName(typeName);
        }
      } else {
        return getPrimitiveClass(type)
            .orElseThrow(() -> new RuntimeException("Unable to find type for " + type.getTypeName()));
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  static java.lang.reflect.Type toType(GenericInfo info) {
    java.lang.reflect.Type[] typeArguments =
        info.getGenerics().stream().map(ASTUtils::toType).toArray(java.lang.reflect.Type[]::new);
    try {
      return TypeToken.getParameterized(Class.forName(info.getConcreteType().getTypeName()), typeArguments).getType();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  Optional<Class> getPrimitiveClass(Type type) {
    return ofNullable(primitiveTypesClasses.get(type.getTypeName()));
  }

  Optional<TypeMirror> getPrimitiveTypeMirror(Class clazz) {
    return ofNullable(primitiveTypeMirrors.get(clazz.getTypeName()));
  }
}
