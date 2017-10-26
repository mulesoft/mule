/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.enricher.stereotypes;


import static java.lang.String.format;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition.NAMESPACE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;

import org.mule.runtime.api.meta.model.declaration.fluent.WithStereotypesDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.annotation.param.stereotype.Validator;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.java.type.WithAnnotations;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Base implementation for objects that given an {@link WithAnnotations annotated element} resolves it's stereotype.
 *
 * @since 4.0
 */
abstract class StereotypeResolver<T extends WithAnnotations> {

  protected final T annotatedElement;
  protected final WithStereotypesDeclaration declaration;
  protected final String namespace;
  protected final StereotypeModel fallbackStereotype;
  protected Validator validatorAnnotation;
  protected Stereotype stereotypeAnnotation;
  protected Map<StereotypeDefinition, StereotypeModel> stereotypesCache;

  protected static StereotypeModel createCustomStereotype(Class<? extends StereotypeDefinition> definitionClass,
                                                          String namespace,
                                                          Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
    try {
      return getStereotype(instantiateClass(definitionClass), namespace, stereotypesCache);
    } catch (Exception e) {
      throw new IllegalModelDefinitionException(
                                                "Invalid StereotypeDefinition found with name: " + definitionClass.getName(),
                                                e);
    }
  }

  protected static StereotypeModel getStereotype(StereotypeDefinition stereotypeDefinition,
                                                 String namespace,
                                                 Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
    return stereotypesCache.computeIfAbsent(stereotypeDefinition, definition -> {

      if (!isValidStereotype(stereotypeDefinition, namespace)) {
        throw new IllegalModelDefinitionException(format(
                                                         "Stereotype '%s' defines namespace '%s' which doesn't match extension stereotype '%s'. No extension can define "
                                                             + "stereotypes on namespaces other than its own",
                                                         stereotypeDefinition.getName(), stereotypeDefinition.getNamespace(),
                                                         namespace));
      }

      final StereotypeModelBuilder builder = newStereotype(stereotypeDefinition.getName(), namespace);
      stereotypeDefinition.getParent().ifPresent(parent -> {
        String parentNamespace = parent.getNamespace();
        if (isBlank(parentNamespace)) {
          parentNamespace = namespace;
        }
        builder.withParent(getStereotype(parent, parentNamespace, stereotypesCache));
      });

      return builder.build();
    });
  }

  private static boolean isValidStereotype(StereotypeDefinition stereotypeDefinition, String namespace) {
    if (isBlank(stereotypeDefinition.getNamespace())) {
      return true;
    }

    return namespace.equals(stereotypeDefinition.getNamespace()) || NAMESPACE.equals(stereotypeDefinition.getNamespace());
  }

  protected StereotypeResolver(T annotatedElement,
                               WithStereotypesDeclaration declaration,
                               String namespace,
                               StereotypeModel fallbackStereotype,
                               Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
    this.annotatedElement = annotatedElement;
    this.declaration = declaration;
    this.namespace = namespace;
    this.stereotypesCache = stereotypesCache;
    this.fallbackStereotype = fallbackStereotype;
    stereotypeAnnotation = getAnnotation(Stereotype.class);
    validatorAnnotation = getAnnotation(Validator.class);

    if (validatorAnnotation != null && stereotypeAnnotation != null) {
      throw new IllegalModelDefinitionException(format("%s is annotated with both @%s and @%s. Only one can "
          + "be provided at the same time for the same component",
                                                       resolveDescription(), Stereotype.class.getSimpleName(),
                                                       Validator.class.getSimpleName()));
    }
  }

  protected abstract <T extends Annotation> T getAnnotation(Class<T> annotationType);

  protected abstract String resolveDescription();

  protected void resolveStereotype() {
    if (validatorAnnotation != null) {
      addValidationStereotype();
    } else if (stereotypeAnnotation != null) {
      declaration.withStereotype(createCustomStereotype());
    } else {
      addFallbackStereotype();
    }
  }

  protected void addFallbackStereotype() {
    declaration.withStereotype(fallbackStereotype);
  }

  protected StereotypeModel createCustomStereotype() {
    return createCustomStereotype(stereotypeAnnotation.value(), namespace, stereotypesCache);
  }

  protected void addValidationStereotype() {
    declaration.withStereotype(newStereotype(VALIDATOR_DEFINITION.getName(), namespace)
        .withParent(MuleStereotypes.VALIDATOR)
        .build());
  }
}
