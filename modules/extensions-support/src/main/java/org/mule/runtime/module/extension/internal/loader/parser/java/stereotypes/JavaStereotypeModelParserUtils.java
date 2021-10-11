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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.SdkStereotypeDefinitionAdapter.from;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.annotation.param.stereotype.Validator;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class JavaStereotypeModelParserUtils {

  public static boolean isValidator(WithAnnotations element) {
    return element.isAnnotatedWith(Validator.class) ||
        element.isAnnotatedWith(org.mule.sdk.api.annotation.param.stereotype.Validator.class);
  }

  public static Optional<StereotypeModel> resolveStereotype(WithAnnotations annotatedElement,
                                                            String elementType,
                                                            String elementName,
                                                            StereotypeModelFactory factory) {
    StereotypeDefinition stereotypeDefinition = mapReduceSingleAnnotation(
                                                                          annotatedElement,
                                                                          elementType,
                                                                          elementName,
                                                                          Stereotype.class,
                                                                          org.mule.sdk.api.annotation.param.stereotype.Stereotype.class,
                                                                          value -> value.getClassValue(Stereotype::value),
                                                                          value -> value
                                                                              .getClassValue(org.mule.sdk.api.annotation.param.stereotype.Stereotype::value))
                                                                                  .flatMap(type -> type.getDeclaringClass())
                                                                                  .map(SdkStereotypeDefinitionAdapter::from)
                                                                                  .orElse(null);


    if (isValidator(annotatedElement)) {
      if (stereotypeDefinition != null) {
        throw new IllegalModelDefinitionException(format("%s '%s' is annotated with both @%s and @%s. Only one can "
            + "be provided at the same time for the same component",
                                                         elementType, elementName, Stereotype.class.getSimpleName(),
                                                         Validator.class.getSimpleName()));
      }

      return of(factory.getValidatorStereotype());
    }

    return stereotypeDefinition != null
        ? of(factory.createStereotype(stereotypeDefinition))
        : empty();
  }

  public static List<StereotypeModel> getAllowedStereotypes(WithAnnotations element,
                                                            WithAnnotations fallbackElement,
                                                            StereotypeModelFactory factory) {

    List<StereotypeModel> stereotypes = getAllowedStereotypes(element, factory);
    if (stereotypes.isEmpty()) {
      stereotypes = JavaStereotypeModelParserUtils.getAllowedStereotypes(fallbackElement, factory);
    }

    return stereotypes;
  }

  public static List<StereotypeModel> getAllowedStereotypes(WithAnnotations element, StereotypeModelFactory factory) {
    return concat(
                  getAllowedTypeStream(element,
                                       AllowedStereotypes.class,
                                       AllowedStereotypes::value),
                  getAllowedTypeStream(element,
                                       org.mule.sdk.api.annotation.param.stereotype.AllowedStereotypes.class,
                                       org.mule.sdk.api.annotation.param.stereotype.AllowedStereotypes::value))
                                           .map(type -> factory.createStereotype(from(type.getDeclaringClass().get())))
                                           .collect(toList());
  }

  public static StereotypeDefinition asDefinition(StereotypeModel model) {
    return new StereotypeDefinition() {

      @Override
      public String getName() {
        return model.getType();
      }

      @Override
      public String getNamespace() {
        return model.getNamespace();
      }

      @Override
      public Optional<StereotypeDefinition> getParent() {
        return model.getParent().map(parent -> asDefinition(parent));
      }
    };
  }

  private static <A extends Annotation> Stream<Type> getAllowedTypeStream(WithAnnotations element,
                                                                          Class<A> annotationType,
                                                                          Function<A, Class[]> mapper) {
    return element.getValueFromAnnotation(annotationType)
        .map(value -> value.getClassArrayValue(mapper).stream()
            .filter(type -> type.getDeclaringClass().isPresent()))
        .orElse(Stream.empty());
  }


  private JavaStereotypeModelParserUtils() {}
}
