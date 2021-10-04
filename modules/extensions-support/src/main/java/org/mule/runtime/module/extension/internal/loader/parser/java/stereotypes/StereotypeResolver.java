/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes;


import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.api.util.FunctionalUtils.computeIfAbsent;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition.NAMESPACE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.mapReduceExtensionAnnotation;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.annotation.param.stereotype.Validator;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

/**
 * Base implementation for objects that given an {@link WithAnnotations annotated element} resolves it's stereotype.
 *
 * @since 4.0
 */
public class StereotypeResolver {

  protected final WithAnnotations annotatedElement;
  protected final String namespace;
  protected boolean isValidator;
  protected StereotypeDefinition stereotypeDefinition;
  protected Map<StereotypeDefinition, StereotypeModel> stereotypesCache;

  public StereotypeResolver() {

  }

  protected static StereotypeModel getStereotype(StereotypeDefinition stereotypeDefinition,
                                                 String namespace,
                                                 Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
    return computeIfAbsent(stereotypesCache, stereotypeDefinition, definition -> {

      if (!isValidStereotype(stereotypeDefinition, namespace)) {
        throw new IllegalModelDefinitionException(format(
                                                         "Stereotype '%s' defines namespace '%s' which doesn't match extension stereotype '%s'. No extension can define "
                                                             + "stereotypes on namespaces other than its own",
                                                         stereotypeDefinition.getName(), stereotypeDefinition.getNamespace(),
                                                         namespace));
      }

      String resolvedNamespace = isBlank(stereotypeDefinition.getNamespace()) ? namespace : stereotypeDefinition.getNamespace();
      final StereotypeModelBuilder builder = newStereotype(stereotypeDefinition.getName(), resolvedNamespace);
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

  private StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition) {
    return computeIfAbsent(stereotypesCache, stereotypeDefinition, definition -> {

      if (!isValidStereotype(stereotypeDefinition, namespace)) {
        throw new IllegalModelDefinitionException(format(
            "Stereotype '%s' defines namespace '%s' which doesn't match extension stereotype '%s'. No extension can define "
                + "stereotypes on namespaces other than its own",
            stereotypeDefinition.getName(), stereotypeDefinition.getNamespace(),
            namespace));
      }

      String resolvedNamespace = isBlank(stereotypeDefinition.getNamespace()) ? namespace : stereotypeDefinition.getNamespace();
      final StereotypeModelBuilder builder = newStereotype(stereotypeDefinition.getName(), resolvedNamespace);
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

  public StereotypeResolver(WithAnnotations annotatedElement,
                               String namespace,
                               String elementType,
                               String elementName,
                               Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
    this.annotatedElement = annotatedElement;
    this.namespace = namespace;
    this.stereotypesCache = stereotypesCache;

    stereotypeDefinition = mapReduceExtensionAnnotation(
        annotatedElement,
        elementType,
        elementName,
        Stereotype.class,
        org.mule.sdk.api.annotation.param.stereotype.Stereotype.class,
        value -> value.getClassValue(Stereotype::value),
        value -> value.getClassValue(org.mule.sdk.api.annotation.param.stereotype.Stereotype::value)
    ).flatMap(type -> type.getDeclaringClass())
        .map(SdkStereotypeDefinitionAdapter::from)
        .orElse(null);

    isValidator = annotatedElement.isAnnotatedWith(Validator.class)
        || annotatedElement.isAnnotatedWith(org.mule.sdk.api.annotation.param.stereotype.Validator.class);

    if (stereotypeDefinition != null && isValidator) {
      throw new IllegalModelDefinitionException(format("%s is annotated with both @%s and @%s. Only one can "
          + "be provided at the same time for the same component",
                                                       resolveDescription(), Stereotype.class.getSimpleName(),
                                                       Validator.class.getSimpleName()));
    }
  }

  protected abstract String resolveDescription();

  public StereotypeResolution resolveStereotype() {
    if (isValidator) {
      return new StereotypeResolution(empty(), true);
    }
    return new StereotypeResolution(of(getStereotype(stereotypeDefinition, namespace, stereotypesCache)), false);
  }
}
