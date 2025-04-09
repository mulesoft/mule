/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
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

/**
 * Utils for parsing Java defined Stereotypes
 *
 * @since 4.5.0
 */
public final class JavaStereotypeModelParserUtils {

  /**
   * @param element an element
   * @return whether the {@code element} is a validator or not
   */
  public static boolean isValidator(WithAnnotations element) {
    return element.isAnnotatedWith(Validator.class) ||
        element.isAnnotatedWith(org.mule.sdk.api.annotation.param.stereotype.Validator.class);
  }

  /**
   * Returns the {@code annotatedElement} {@link StereotypeModel} if one is defined
   *
   * @param annotatedElement an element
   * @param elementType      the type of element
   * @param elementName      the name of the element
   * @param factory          the factory used to create the stereotype
   * @return an {@link StereotypeModel} if one is defined
   */
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

    if (stereotypeDefinition != null) {
      return of(factory.createStereotype(stereotypeDefinition));
    }

    return empty();
  }

  /**
   * Returns The element's allowed stereotypes. If none is defined, the {@code fallbackElement} is tested instead.
   *
   * @param element         an element
   * @param fallbackElement the fallback element
   * @param factory         the factory used to create the stereotype
   * @return a {@link List} of {@link StereotypeModel}. Might be empty but will never be {@code null}
   */
  public static List<StereotypeModel> getAllowedStereotypes(WithAnnotations element,
                                                            WithAnnotations fallbackElement,
                                                            StereotypeModelFactory factory) {

    List<StereotypeModel> stereotypes = getAllowedStereotypes(element, factory);
    if (stereotypes.isEmpty()) {
      stereotypes = getAllowedStereotypes(fallbackElement, factory);
    }

    return stereotypes;
  }

  /**
   * Returns The element's allowed stereotypes. If none is defined, the {@code fallbackElement} is tested instead.
   *
   * @param element an element
   * @param factory the factory used to create the stereotype
   * @return a {@link List} of {@link StereotypeModel}. Might be empty but will never be {@code null}
   */
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

  /**
   * Translates the given {@code model} into a {@link StereotypeDefinition}
   *
   * @param model a {@link StereotypeModel}
   * @return a {@link StereotypeDefinition}
   */
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
