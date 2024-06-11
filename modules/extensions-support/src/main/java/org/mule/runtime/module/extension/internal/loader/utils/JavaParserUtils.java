/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.ExternalLibraryType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.lang.model.element.Element;

/**
 * Utilities for parsing Extensions defined through the Java language.
 * <p>
 * Use these methods when you're sure that you're parsing compiled classes. Do not use when the parsing context includes an AST
 * (either java's or Mule's).
 * <p>
 * This class is not part of the API and should not be used by anyone (or anything) but the runtime. Backwards compatibility not
 * guaranteed on this class.
 *
 * @since 4.8.0
 */
public final class JavaParserUtils {

  private JavaParserUtils() {}

  /**
   * @param field a java Field
   * @return the field's alias, as defined by any of the {@code @Alias} annotations
   */
  public static String getAlias(Field field) {
    return getAlias(field, field::getName);
  }

  /**
   * @param clazz a Java class
   * @return the class alias, as defined by any of the {@code @Alias} annotations
   */
  public static String getAlias(Class<?> clazz) {
    return getAlias(clazz, clazz::getSimpleName);
  }

  /**
   * Searches the given {@code element} for any of the {@code @Alias} annotations. If any are found, the resolved alias is
   * returned. Otherwise, the {@code defaultValue} output is returned
   *
   * @param element      an annotated element
   * @param defaultValue a default value supplier
   * @return the resolved alias.
   */
  public static String getAlias(AnnotatedElement element, Supplier<String> defaultValue) {
    return getAlias(element::getAnnotation, defaultValue);
  }

  /**
   * Runs the {@code annotationMapper} through the {@code @Alias} annotations. If any are found, the resolved alias is returned.
   * Otherwise, the {@code defaultValue} output is returned
   *
   * @param annotationMapper a function which encapsulates the annotation resolution
   * @param defaultValue     a default value supplier
   * @return the resolved alias
   */
  public static String getAlias(Function<Class<? extends Annotation>, Annotation> annotationMapper,
                                Supplier<String> defaultValue) {
    String name = null;
    Alias legacyAlias = (Alias) annotationMapper.apply(Alias.class);
    if (legacyAlias != null) {
      name = legacyAlias.value();
    } else {
      org.mule.sdk.api.annotation.Alias alias =
          (org.mule.sdk.api.annotation.Alias) annotationMapper.apply(org.mule.sdk.api.annotation.Alias.class);
      if (alias != null) {
        name = alias.value();
      }
    }

    return name == null || name.length() == 0 ? defaultValue.get() : name;
  }

  /**
   * @param element an Annotated element
   * @return the {@link ExpressionSupport} defined for the element, if defined. {@link Optional#empty()} otherwise.
   */
  public static Optional<ExpressionSupport> getExpressionSupport(AnnotatedElement element) {
    return getExpressionSupport(element::getAnnotation);
  }

  /**
   * @param mapper function which encapsulates annotation resolution
   * @return the {@link ExpressionSupport} defined for the element, if defined. {@link Optional#empty()} otherwise.
   */
  public static Optional<ExpressionSupport> getExpressionSupport(Function<Class<? extends Annotation>, ? extends Annotation> mapper) {
    return mapReduceAnnotation(mapper,
                               Expression.class,
                               org.mule.sdk.api.annotation.Expression.class,
                               ann -> ann.value(),
                               ann -> toMuleApi(ann.value()));
  }

  /**
   * @param field a java Field
   * @return {@code true} if the field is annotated with {@link ConfigOverride} or
   *         {@link org.mule.sdk.api.annotation.param.ConfigOverride}
   */
  public static boolean isConfigOverride(Field field) {
    ConfigOverride legacyOverride = field.getAnnotation(ConfigOverride.class);
    org.mule.sdk.api.annotation.param.ConfigOverride sdkOverride =
        field.getAnnotation(org.mule.sdk.api.annotation.param.ConfigOverride.class);
    return legacyOverride != null || sdkOverride != null;
  }

  public static Optional<Class<?>> getNullSafeDefaultImplementedType(Field field) {
    return mapReduceAnnotation(field::getAnnotation,
                               NullSafe.class,
                               org.mule.sdk.api.annotation.param.NullSafe.class,
                               NullSafe::defaultImplementingType,
                               org.mule.sdk.api.annotation.param.NullSafe::defaultImplementingType);
  }

  /**
   * Monad for extracting information from an {@link Element} which might be annotated with two different annotations of similar
   * semantics. Both annotations types are reduced to a single output type.
   * <p>
   * Simultaneous presence of both types will be considered an error
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
                                                                                                Element element,
                                                                                                Class<R> legacyAnnotationClass,
                                                                                                Class<S> sdkAnnotationClass,
                                                                                                Function<R, T> legacyAnnotationMapping,
                                                                                                Function<S, T> sdkAnnotationMapping) {

    return mapReduceAnnotation(element::getAnnotation,
                               legacyAnnotationClass,
                               sdkAnnotationClass,
                               legacyAnnotationMapping,
                               sdkAnnotationMapping);
  }

  /**
   * Transforms an sdk-api {@link org.mule.sdk.api.meta.ExpressionSupport} into a mule-api {@link ExpressionSupport}
   *
   * @param support an sdk-api representation of the expression support semantic
   * @return the transformed value
   * @throws IllegalModelDefinitionException if no equivalent semantic found.
   */
  public static ExpressionSupport toMuleApi(org.mule.sdk.api.meta.ExpressionSupport support) {
    if (support == org.mule.sdk.api.meta.ExpressionSupport.SUPPORTED) {
      return ExpressionSupport.SUPPORTED;
    } else if (support == org.mule.sdk.api.meta.ExpressionSupport.NOT_SUPPORTED) {
      return ExpressionSupport.NOT_SUPPORTED;
    } else if (support == org.mule.sdk.api.meta.ExpressionSupport.REQUIRED) {
      return ExpressionSupport.REQUIRED;
    } else {
      throw new IllegalModelDefinitionException("Unsupported expression support type " + support);
    }
  }

  /**
   * Transforms an sdk-api {@link org.mule.sdk.api.meta.Category} into a mule-api {@link Category}
   *
   * @param category an sdk-api representation of the Category semantic
   * @return the transformed value
   * @throws IllegalModelDefinitionException if no equivalent semantic found.
   */
  public static Category toMuleApi(org.mule.sdk.api.meta.Category category) {
    if (category == org.mule.sdk.api.meta.Category.SELECT) {
      return Category.SELECT;
    } else if (category == org.mule.sdk.api.meta.Category.COMMUNITY) {
      return Category.COMMUNITY;
    } else if (category == org.mule.sdk.api.meta.Category.CERTIFIED) {
      return Category.CERTIFIED;
    } else if (category == org.mule.sdk.api.meta.Category.PREMIUM) {
      return Category.PREMIUM;
    } else {
      throw new IllegalModelDefinitionException("Unsupported Category type " + category);
    }
  }

  private static <R extends Annotation, S extends Annotation, T> Optional<T> mapReduceAnnotation(
                                                                                                 Function<Class<? extends Annotation>, ? extends Annotation> mapper,
                                                                                                 Class<R> legacyAnnotationClass,
                                                                                                 Class<S> sdkAnnotationClass,
                                                                                                 Function<R, T> legacyAnnotationMapping,
                                                                                                 Function<S, T> sdkAnnotationMapping) {

    R legacyAnnotation = (R) mapper.apply(legacyAnnotationClass);
    S sdkAnnotation = (S) mapper.apply(sdkAnnotationClass);

    Optional<T> result;

    if (legacyAnnotation != null) {
      result = ofNullable(legacyAnnotationMapping.apply(legacyAnnotation));
    } else if (sdkAnnotation != null) {
      result = ofNullable(sdkAnnotationMapping.apply(sdkAnnotation));
    } else {
      result = empty();
    }

    return result;
  }
}
