/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static java.util.Collections.emptyList;

import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.ConfigReferences;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches the {@link ParameterDeclaration}s of an extension model with a {@link List} of {@link StereotypeModel} if they are
 * marked as a reference to at least some element.
 *
 * @since 4.0
 */
public final class ParameterAllowedStereotypesDeclarionEnricher extends AbstractAnnotatedDeclarationEnricher {

  /**
   * {@inheritDoc}
   * <p>
   * Checks all the declared parameters if someone is annotated with {@link ConfigReferences} to create the references and set
   * them up.
   */
  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new Enricher().enrich(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private static class Enricher {

    void enrich(ExtensionDeclaration extension) {
      Map<String, ObjectType> typesByClassName = new HashMap<>();
      extension.getTypes()
          .forEach(type -> type.getAnnotation(ClassInformationAnnotation.class)
              .ifPresent(st -> typesByClassName.put(st.getClassname(), type)));

      new IdempotentDeclarationWalker() {

        @Override
        protected void onParameter(ParameterGroupDeclaration parameterGroup, ParameterDeclaration declaration) {
          declaration.getModelProperty(ImplementingParameterModelProperty.class)
              .ifPresent(param -> declaration.getAllowedStereotypeModels()
                  .addAll(getStereotypes(param.getParameter(), typesByClassName)));
          declaration.getModelProperty(DeclaringMemberModelProperty.class)
              .ifPresent(field -> declaration.getAllowedStereotypeModels()
                  .addAll(getStereotypes(field.getDeclaringField(), typesByClassName)));
        }
      }.walk(extension);
    }

    private List<StereotypeModel> getStereotypes(AnnotatedElement element, Map<String, ObjectType> typesByClassName) {
      Type parameterizedType;
      Class<?> paramType;
      if (element instanceof Field) {
        parameterizedType = ((Field) element).getGenericType();
        paramType = ((Field) element).getType();
      } else {
        parameterizedType = ((Parameter) element).getParameterizedType();
        paramType = ((Parameter) element).getType();
      }

      if (Collection.class.isAssignableFrom(paramType)) {
        return parameterizedTypeAnnotations(typesByClassName, parameterizedType, 0);
      } else if (Map.class.isAssignableFrom(paramType)) {
        return parameterizedTypeAnnotations(typesByClassName, parameterizedType, 1);
      } else if (paramType.isEnum()) {
        return emptyList();
      } else {
        return typeToAnnotations(typesByClassName, paramType);
      }
    }

    private List<StereotypeModel> parameterizedTypeAnnotations(Map<String, ObjectType> typesByClassName,
                                                               Type parameterizedType,
                                                               final int typeArgumentIndex) {
      if (parameterizedType instanceof ParameterizedType) {
        final Type[] actualTypeArguments = ((ParameterizedType) parameterizedType).getActualTypeArguments();

        Class<?> mapParamType;

        if (actualTypeArguments[typeArgumentIndex] instanceof ParameterizedType) {
          mapParamType = (Class<?>) ((ParameterizedType) actualTypeArguments[typeArgumentIndex]).getRawType();
        } else if (actualTypeArguments[typeArgumentIndex] instanceof WildcardType) {
          mapParamType = Object.class;
        } else {
          mapParamType = (Class<?>) actualTypeArguments[typeArgumentIndex];
        }

        return typeToAnnotations(typesByClassName, mapParamType);
      } else {
        return emptyList();
      }
    }

    private List<StereotypeModel> typeToAnnotations(Map<String, ObjectType> typesByClassName, Class<?> paramType) {
      if (!typesByClassName.containsKey(paramType.getCanonicalName())) {
        return emptyList();
      } else {
        return typesByClassName.get(paramType.getCanonicalName()).getAnnotation(StereotypeTypeAnnotation.class)
            .map(st -> st.getAllowedStereotypes())
            .orElse(emptyList());
      }
    }
  }
}
