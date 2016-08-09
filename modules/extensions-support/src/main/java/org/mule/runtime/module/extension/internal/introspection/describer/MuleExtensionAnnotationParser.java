/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.property.LayoutModelProperty;
import org.mule.runtime.extension.api.introspection.property.LayoutModelPropertyBuilder;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithAnnotations;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.DefaultExceptionEnricherFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


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

  public static Extension getExtension(Class<?> extensionType) {
    Extension extension = extensionType.getAnnotation(Extension.class);
    checkState(extension != null, String.format("%s is not a Mule extension since it's not annotated with %s",
                                                extensionType.getName(), Extension.class.getName()));
    return extension;
  }

  public static <T extends Annotation> List<T> parseRepeatableAnnotation(Class<?> extensionType, Class<T> annotation,
                                                                         Function<Annotation, T[]> containerConsumer) {
    List<T> annotationDeclarations = ImmutableList.of();

    Repeatable repeatableContainer = annotation.getAnnotation(Repeatable.class);
    if (repeatableContainer != null) {
      Annotation container = IntrospectionUtils.getAnnotation(extensionType, repeatableContainer.value());
      if (container != null) {
        annotationDeclarations = ImmutableList.copyOf(containerConsumer.apply(container));
      }
    }

    T singleDeclaration = IntrospectionUtils.getAnnotation(extensionType, annotation);
    if (singleDeclaration != null) {
      annotationDeclarations = ImmutableList.of(singleDeclaration);
    }

    return annotationDeclarations;
  }

  public static List<String> getParamNames(Method method) {
    ImmutableList.Builder<String> paramNames = ImmutableList.builder();
    for (java.lang.reflect.Parameter parameter : method.getParameters()) {
      paramNames.add(parameter.getName());
    }

    return paramNames.build();
  }

  public static Map<Class<? extends Annotation>, Annotation> toMap(Annotation[] annotations) {

    Map<Class<? extends Annotation>, Annotation> map = new HashMap<>();

    for (Annotation annotation : annotations) {
      map.put(ClassUtils.resolveAnnotationClass(annotation), annotation);
    }

    return map;
  }

  private static void parseLayoutAnnotations(AnnotatedElement annotatedElement, LayoutModelPropertyBuilder builder) {
    Password passwordAnnotation = annotatedElement.getAnnotation(Password.class);
    if (passwordAnnotation != null) {
      builder.withPassword(true);
    }
    Text textAnnotation = annotatedElement.getAnnotation(Text.class);
    if (textAnnotation != null) {
      builder.withText(true);
    }
  }

  private static void parseLayoutAnnotations(WithAnnotations annotatedElement, LayoutModelPropertyBuilder builder) {
    java.util.Optional<Password> passwordAnnotation = annotatedElement.getAnnotation(Password.class);
    if (passwordAnnotation.isPresent()) {
      builder.withPassword(true);
    }
    java.util.Optional<Text> textAnnotation = annotatedElement.getAnnotation(Text.class);
    if (textAnnotation.isPresent()) {
      builder.withText(true);
    }
  }

  private static void parsePlacementAnnotation(AnnotatedElement annotatedElement, LayoutModelPropertyBuilder builder) {
    Placement placementAnnotation = annotatedElement.getAnnotation(Placement.class);
    if (placementAnnotation != null) {
      builder.order(placementAnnotation.order()).groupName(placementAnnotation.group()).tabName(placementAnnotation.tab());
    }
  }

  private static void parsePlacementAnnotation(WithAnnotations annotatedElement, LayoutModelPropertyBuilder builder) {
    java.util.Optional<Placement> placementAnnotation = annotatedElement.getAnnotation(Placement.class);
    if (placementAnnotation.isPresent()) {
      Placement placement = placementAnnotation.get();
      builder.order(placement.order()).groupName(placement.group()).tabName(placement.tab());
    }
  }

  static LayoutModelProperty parseLayoutAnnotations(AnnotatedElement annotatedElement, String name) {
    return parseLayoutAnnotations(annotatedElement, name, LayoutModelPropertyBuilder.create());
  }

  static LayoutModelProperty parseLayoutAnnotations(WithAnnotations annotatedElement, String name) {
    return parseLayoutAnnotations(annotatedElement, name, LayoutModelPropertyBuilder.create());
  }

  static LayoutModelProperty parseLayoutAnnotations(WithAnnotations annotatedElement, String name,
                                                    LayoutModelPropertyBuilder builder) {
    if (isDisplayAnnotationPresent(annotatedElement)) {
      parseLayoutAnnotations(annotatedElement, builder);
      parsePlacementAnnotation(annotatedElement, builder);
      return builder.build();
    }
    return null;
  }

  static LayoutModelProperty parseLayoutAnnotations(AnnotatedElement annotatedElement, String name,
                                                    LayoutModelPropertyBuilder builder) {
    if (isDisplayAnnotationPresent(annotatedElement)) {
      parseLayoutAnnotations(annotatedElement, builder);
      parsePlacementAnnotation(annotatedElement, builder);
      return builder.build();
    }
    return null;
  }

  private static boolean isDisplayAnnotationPresent(AnnotatedElement annotatedElement) {
    List<Class> displayAnnotations = Arrays.asList(Password.class, Text.class, Placement.class);
    return displayAnnotations.stream().anyMatch(annotation -> annotatedElement.getAnnotation(annotation) != null);
  }

  private static boolean isDisplayAnnotationPresent(WithAnnotations annotatedElement) {
    List<Class> displayAnnotations = Arrays.asList(Password.class, Text.class, Placement.class);
    return displayAnnotations.stream().anyMatch(annotation -> annotatedElement.getAnnotation(annotation) != null);
  }

  /**
   * Enriches the {@link ParameterDeclarer} with a {@link MetadataKeyPartModelProperty} or a {@link MetadataContentModelProperty}
   * if the parsedParameter is annotated either as {@link MetadataKeyId}, {@link MetadataKeyPart} or {@link Content} respectibly.
   *
   * @param element the method annotated parameter parsed
   * @param baseDeclaration the {@link ParameterDeclarer} associated to the parsed parameter
   */
  public static void parseMetadataAnnotations(AnnotatedElement element, BaseDeclaration baseDeclaration) {
    if (element.isAnnotationPresent(Content.class)) {
      baseDeclaration.addModelProperty(new MetadataContentModelProperty());
    }

    if (element.isAnnotationPresent(MetadataKeyId.class)) {
      baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(1));
    }

    if (element.isAnnotationPresent(MetadataKeyPart.class)) {
      MetadataKeyPart metadataKeyPart = element.getAnnotation(MetadataKeyPart.class);
      baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(metadataKeyPart.order()));
    }
  }

  static java.util.Optional<ExceptionEnricherFactory> getExceptionEnricherFactory(WithAnnotations element) {
    final java.util.Optional<OnException> onExceptionAnnotation = element.getAnnotation(OnException.class);
    if (onExceptionAnnotation.isPresent()) {
      return java.util.Optional.of(new DefaultExceptionEnricherFactory(onExceptionAnnotation.get().value()));
    }
    return java.util.Optional.empty();
  }
}
