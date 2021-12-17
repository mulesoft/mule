/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.internal.loader.util.JavaParserUtils.getAlias;
import static org.mule.runtime.extension.internal.loader.util.JavaParserUtils.toMuleApi;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.display.LayoutModel.LayoutModelBuilder;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.java.info.ExtensionInfo;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * Utilities for reading annotations as a mean to describe extensions
 *
 * @since 3.7.0
 */
public final class MuleExtensionAnnotationParser {

  public static String getMemberName(BaseDeclaration<?> declaration, String defaultName) {
    return declaration.getModelProperty(DeclaringMemberModelProperty.class).map(p -> p.getDeclaringField().getName())
        .orElse(defaultName);
  }

  public static ExtensionInfo getExtensionInfo(Class<?> extensionType) {
    Extension legacy = extensionType.getAnnotation(Extension.class);
    if (legacy != null) {
      return new ExtensionInfo(legacy.name(), legacy.vendor(), legacy.category());
    } else {
      org.mule.sdk.api.annotation.Extension extension = extensionType.getAnnotation(org.mule.sdk.api.annotation.Extension.class);
      if (extension != null) {
        return new ExtensionInfo(extension.name(), extension.vendor(), toMuleApi(extension.category()));
      }
    }

    throw new IllegalModelDefinitionException(String.format("Class '%s' not annotated with neither '%s' nor '%s'",
                                                            extensionType.getName(), Extension.class.getName(),
                                                            org.mule.sdk.api.annotation.Extension.class.getName()));
  }

  public static <L extends Annotation, A extends Annotation, T> Stream<T> mapReduceRepeatableAnnotation(
                                                                                                        WithAnnotations element,
                                                                                                        Class<L> legacyAnnotationType,
                                                                                                        Class<A> sdkApiAnnotationType,
                                                                                                        Function<Annotation, L[]> legacyContainerMapping,
                                                                                                        Function<Annotation, A[]> sdkApiContainerMapping,
                                                                                                        Function<AnnotationValueFetcher<L>, T> legacyMapping,
                                                                                                        Function<AnnotationValueFetcher<A>, T> sdkApiMapping) {

    return Stream.concat(
                         mapReduceRepeatableAnnotation(element, legacyAnnotationType, legacyContainerMapping).map(legacyMapping),
                         mapReduceRepeatableAnnotation(element, sdkApiAnnotationType, sdkApiContainerMapping).map(sdkApiMapping));
  }

  public static <T extends Annotation> Stream<AnnotationValueFetcher<T>> mapReduceRepeatableAnnotation(WithAnnotations element,
                                                                                                       Class<T> annotation,
                                                                                                       Function<Annotation, T[]> containerMapper) {

    Stream<AnnotationValueFetcher<T>> singleElementStream = getAnnotationFromHierarchy(element, annotation)
        .map(Stream::of)
        .orElse(Stream.empty());

    Repeatable repeatableContainer = annotation.getAnnotation(Repeatable.class);
    if (repeatableContainer != null) {
      Stream<AnnotationValueFetcher<T>> containerStream = getAnnotationFromHierarchy(element, repeatableContainer.value())
          .map(container -> container.getInnerAnnotations((Function) containerMapper).stream())
          .orElse(Stream.empty());

      return Stream.concat(singleElementStream, containerStream);
    } else {
      return singleElementStream;
    }
  }

  private static <T extends Annotation> Optional<AnnotationValueFetcher<T>> getAnnotationFromHierarchy(WithAnnotations element,
                                                                                                       Class<T> annotation) {
    Optional<AnnotationValueFetcher<T>> valueFetcher = element.getValueFromAnnotation(annotation);
    if (valueFetcher.isPresent()) {
      return valueFetcher;
    }

    if (element instanceof Type) {
      return ((Type) element).getSuperType().flatMap(superType -> getAnnotationFromHierarchy(superType, annotation));
    }

    return empty();
  }

  public static List<String> getParamNames(Method method) {
    return Stream.of(method.getParameters())
        .map(parameter -> getAlias(parameter, parameter::getName))
        .collect(toList());
  }

  public static Map<Class<? extends Annotation>, Annotation> toMap(Annotation[] annotations) {

    Map<Class<? extends Annotation>, Annotation> map = new HashMap<>();

    for (Annotation annotation : annotations) {
      map.put(ClassUtils.resolveAnnotationClass(annotation), annotation);
    }

    return map;
  }

  private static void doParseLayoutAnnotations(AnnotatedElement annotatedElement, LayoutModelBuilder builder) {
    if (annotatedElement.getAnnotation(Password.class) != null
        || annotatedElement.getAnnotation(org.mule.sdk.api.annotation.semantics.security.Password.class) != null) {
      builder.asPassword();

    }
    Text textAnnotation = annotatedElement.getAnnotation(Text.class);
    if (textAnnotation != null) {
      builder.asText();
    }
  }

  private static void doParseLayoutAnnotations(WithAnnotations annotatedElement, LayoutModelBuilder builder, String elementName) {
    java.util.Optional<Password> passwordAnnotation = annotatedElement.getAnnotation(Password.class);
    if (passwordAnnotation.isPresent()) {
      builder.asPassword();
    }

    java.util.Optional<Text> legacyTextAnnotation = annotatedElement.getAnnotation(Text.class);
    java.util.Optional<org.mule.sdk.api.annotation.param.display.Text> sdkTextAnnotation =
        annotatedElement.getAnnotation(org.mule.sdk.api.annotation.param.display.Text.class);

    if (legacyTextAnnotation.isPresent() && sdkTextAnnotation.isPresent()) {
      throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                elementName,
                                                                Text.class.getName(),
                                                                org.mule.sdk.api.annotation.param.display.Text.class.getName()));
    } else if (legacyTextAnnotation.isPresent() || sdkTextAnnotation.isPresent()) {
      builder.asText();
    }
  }

  private static void parsePlacementAnnotation(WithAnnotations annotatedElement, LayoutModelBuilder builder, String elementName) {
    java.util.Optional<Placement> legacyPlacementAnnotation = annotatedElement.getAnnotation(Placement.class);
    java.util.Optional<org.mule.sdk.api.annotation.param.display.Placement> sdkPlacementAnnotation =
        annotatedElement.getAnnotation(org.mule.sdk.api.annotation.param.display.Placement.class);

    if (legacyPlacementAnnotation.isPresent() && sdkPlacementAnnotation.isPresent()) {
      throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                elementName,
                                                                Placement.class.getName(),
                                                                org.mule.sdk.api.annotation.param.display.Placement.class
                                                                    .getName()));
    } else if (legacyPlacementAnnotation.isPresent()) {
      int order = legacyPlacementAnnotation.get().order();
      String tab = legacyPlacementAnnotation.get().tab();
      builder.order(order).tabName(tab);
    } else if (sdkPlacementAnnotation.isPresent()) {
      int order = sdkPlacementAnnotation.get().order();
      String tab = sdkPlacementAnnotation.get().tab();
      builder.order(order).tabName(tab);
    }
  }

  private static void parsePlacementAnnotation(AnnotatedElement annotatedElement, LayoutModelBuilder builder,
                                               String elementName) {
    Placement legacyPlacementAnnotation = annotatedElement.getAnnotation(Placement.class);
    org.mule.sdk.api.annotation.param.display.Placement sdkPlacementAnnotation =
        annotatedElement.getAnnotation(org.mule.sdk.api.annotation.param.display.Placement.class);

    if (legacyPlacementAnnotation != null && sdkPlacementAnnotation != null) {
      throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                elementName,
                                                                Placement.class.getName(),
                                                                org.mule.sdk.api.annotation.param.display.Placement.class
                                                                    .getName()));
    } else if (legacyPlacementAnnotation != null) {
      int order = legacyPlacementAnnotation.order();
      String tab = legacyPlacementAnnotation.tab();
      builder.order(order).tabName(tab);
    } else if (sdkPlacementAnnotation != null) {
      int order = sdkPlacementAnnotation.order();
      String tab = sdkPlacementAnnotation.tab();
      builder.order(order).tabName(tab);
    }
  }

  public static Optional<LayoutModel> parseLayoutAnnotations(AnnotatedElement annotatedElement, String elementName) {
    return parseLayoutAnnotations(annotatedElement, LayoutModel.builder(), elementName);
  }

  public static Optional<LayoutModel> parseLayoutAnnotations(WithAnnotations annotatedElement, String elementName) {
    return parseLayoutAnnotations(annotatedElement, LayoutModel.builder(), elementName);
  }

  public static Optional<LayoutModel> parseLayoutAnnotations(WithAnnotations annotatedElement, LayoutModelBuilder builder,
                                                             String elementName) {
    if (isDisplayAnnotationPresent(annotatedElement)) {
      doParseLayoutAnnotations(annotatedElement, builder, elementName);
      parsePlacementAnnotation(annotatedElement, builder, elementName);
      return of(builder.build());
    }
    return empty();
  }

  public static Optional<LayoutModel> parseLayoutAnnotations(AnnotatedElement annotatedElement, LayoutModelBuilder builder,
                                                             String elementName) {
    if (isDisplayAnnotationPresent(annotatedElement)) {
      doParseLayoutAnnotations(annotatedElement, builder);
      parsePlacementAnnotation(annotatedElement, builder, elementName);
      return of(builder.build());
    }
    return empty();
  }

  private static boolean isDisplayAnnotationPresent(AnnotatedElement annotatedElement) {
    List<Class> displayAnnotations = Arrays.asList(Password.class, Text.class, Placement.class,
                                                   org.mule.sdk.api.annotation.param.display.Text.class,
                                                   org.mule.sdk.api.annotation.param.display.Placement.class);
    return displayAnnotations.stream().anyMatch(annotation -> annotatedElement.getAnnotation(annotation) != null);
  }

  private static boolean isDisplayAnnotationPresent(WithAnnotations annotatedElement) {
    List<Class> displayAnnotations = Arrays.asList(Password.class, Text.class, Placement.class,
                                                   org.mule.sdk.api.annotation.param.display.Text.class,
                                                   org.mule.sdk.api.annotation.param.display.Placement.class);
    return displayAnnotations.stream().anyMatch(annotation -> annotatedElement.getAnnotation(annotation) != null);
  }

  /**
   * Monad for extracting information from an {@link ExtensionElement} {@code element} which might be annotated with two different
   * annotations of similar semantics. Both annotations types are reduced to a single output type.
   * <p>
   * Simultaneous presence of both types will be considered an error
   *
   * @param extensionElement        the extension element
   * @param legacyAnnotationClass   the legacy annotation type
   * @param sdkAnnotationClass      the new annotation type
   * @param legacyAnnotationMapping mapping function for the legacy annotation
   * @param sdkAnnotationMapping    mapping function for the new annotation
   * @param <R>                     Legacy annotation's generic type
   * @param <S>                     New annotation's generic type
   * @param <T>                     Output generic type
   * @return a reduced value
   */
  public static <R extends Annotation, S extends Annotation, T> Optional<T> mapReduceSingleAnnotation(
                                                                                                      ExtensionElement extensionElement,
                                                                                                      Class<R> legacyAnnotationClass,
                                                                                                      Class<S> sdkAnnotationClass,
                                                                                                      Function<AnnotationValueFetcher<R>, T> legacyAnnotationMapping,
                                                                                                      Function<AnnotationValueFetcher<S>, T> sdkAnnotationMapping) {

    return mapReduceAnnotation(
                               extensionElement,
                               legacyAnnotationClass,
                               sdkAnnotationClass,
                               legacyAnnotationMapping,
                               sdkAnnotationMapping,
                               () -> new IllegalParameterModelDefinitionException(format("Extension '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                                         extensionElement.getName(),
                                                                                         legacyAnnotationClass.getName(),
                                                                                         sdkAnnotationClass.getName())));
  }

  public static <R extends Annotation, S extends Annotation, T> Optional<T> mapReduceSingleAnnotation(
                                                                                                      WithAnnotations element,
                                                                                                      String elementType,
                                                                                                      String elementName,
                                                                                                      Class<R> legacyAnnotationClass,
                                                                                                      Class<S> sdkAnnotationClass,
                                                                                                      Function<AnnotationValueFetcher<R>, T> legacyAnnotationMapping,
                                                                                                      Function<AnnotationValueFetcher<S>, T> sdkAnnotationMapping) {

    return mapReduceAnnotation(
                               element,
                               legacyAnnotationClass,
                               sdkAnnotationClass,
                               legacyAnnotationMapping,
                               sdkAnnotationMapping,
                               () -> new IllegalParameterModelDefinitionException(format("Annotations %s and %s are both present at the same time on %s %s",
                                                                                         legacyAnnotationClass.getName(),
                                                                                         sdkAnnotationClass.getName(),
                                                                                         elementType, elementName)));
  }

  /**
   * Monad for extracting information from a {@link WithAnnotations} {@code element} which might be annotated with two different
   * annotations of similar semantics. Both annotations types are reduced to a single output type.
   * <p>
   * Simultaneous presence of both types will result in an {@link Optional#empty()} value
   *
   * @param element                 the annotated element
   * @param legacyAnnotationClass   the legacy annotation type
   * @param sdkAnnotationClass      the new annotation type
   * @param legacyAnnotationMapping mapping function for the legacy annotation
   * @param sdkAnnotationMapping    mapping function for the new annotation
   * @param <R>                     Legacy annotation's generic type
   * @param <S>                     New annotation's generic type
   * @param <T>                     Output generic type
   * @return a reduced value
   */
  public static <R extends Annotation, S extends Annotation, T> Optional<T> mapReduceAnnotation(
                                                                                                WithAnnotations element,
                                                                                                Class<R> legacyAnnotationClass,
                                                                                                Class<S> sdkAnnotationClass,
                                                                                                Function<AnnotationValueFetcher<R>, T> legacyAnnotationMapping,
                                                                                                Function<AnnotationValueFetcher<S>, T> sdkAnnotationMapping,
                                                                                                Supplier<? extends IllegalModelDefinitionException> dualDefinitionExceptionFactory) {

    Optional<AnnotationValueFetcher<R>> legacyAnnotation = element.getValueFromAnnotation(legacyAnnotationClass);
    Optional<AnnotationValueFetcher<S>> sdkAnnotation = element.getValueFromAnnotation(sdkAnnotationClass);

    if (legacyAnnotation.isPresent() && sdkAnnotation.isPresent()) {
      throw dualDefinitionExceptionFactory.get();
    } else if (legacyAnnotation.isPresent()) {
      return legacyAnnotation.map(legacyAnnotationMapping);
    } else if (sdkAnnotation.isPresent()) {
      return sdkAnnotation.map(sdkAnnotationMapping);
    } else if (element instanceof Type) {
      return ((Type) element).getSuperType()
          .flatMap(superType -> mapReduceAnnotation(superType, legacyAnnotationClass, sdkAnnotationClass,
                                                    legacyAnnotationMapping, sdkAnnotationMapping,
                                                    dualDefinitionExceptionFactory));
    } else {
      return empty();
    }
  }
}
