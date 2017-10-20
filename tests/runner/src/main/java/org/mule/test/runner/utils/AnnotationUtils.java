/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import static java.util.Collections.addAll;
import static org.apache.commons.lang3.ClassUtils.getAllInterfaces;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for annotations related stuff.
 *
 * @since 4.0
 */
public class AnnotationUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationUtils.class);

  private AnnotationUtils() {}

  /**
   * Looks for the {@link Annotation} in the {@link Class} and invokes the {@link Method}. It will return the result of the
   * invocation. If there {@link Class} is not annotated it will return the default value from the {@link Annotation}.
   *
   * @param klass the {@link Class} where to look for the annotation
   * @param annotationClass the {@link Annotation} class to look for
   * @param methodName the method name of the annotation to be called
   * @param <T> the result of the invocation
   * @throws IllegalStateException if the method is not defined in the annotation
   * @return the attribute from the annotation for the given class
   */
  public static <T> T getAnnotationAttributeFrom(Class<?> klass, Class<? extends Annotation> annotationClass, String methodName) {
    T extensions;
    Annotation annotation = klass.getAnnotation(annotationClass);
    Method method;
    try {
      method = annotationClass.getMethod(methodName);

      if (annotation != null) {
        extensions = (T) method.invoke(annotation);
      } else {
        extensions = (T) method.getDefaultValue();

      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read default " + methodName + " from " + annotationClass);
    }

    return extensions;
  }

  /**
   * Looks for the {@link Annotation} in the {@link Class} and invokes the {@link Method} in the whole hierarhcy until it reaches
   * {@link Object}. It will return a {@link List<T>} with the results of each invocation. If there {@link Class} is not annotated
   * it will return the default value from the {@link Annotation}.
   *
   * @param klass the {@link Class} where to look for the annotation
   * @param annotationClass the {@link Annotation} class to look for
   * @param methodName the method name of the annotation to be called
   * @param <T> the result of the invocation
   * @throws IllegalStateException if the method is not defined in the annotation
   * @return a (@link List} of T for the attributes annotated in all the object hierarchy until it reaches {@link Object} class.
   */
  public static <T> List<T> getAnnotationAttributeFromHierarchy(Class<?> klass, Class<? extends Annotation> annotationClass,
                                                                String methodName) {
    List<T> list = new ArrayList<>();
    Class<?> currentClass = klass;
    while (currentClass != Object.class) {
      T attributeFrom = getAnnotationAttributeFrom(currentClass, annotationClass, methodName);
      if (attributeFrom != null) {
        list.add(attributeFrom);
      }
      currentClass = currentClass.getSuperclass();
    }
    getAllInterfaces(klass).forEach(currentInterfaceClass -> {
      T attributeFrom = getAnnotationAttributeFrom(currentInterfaceClass, annotationClass, methodName);
      if (attributeFrom != null) {
        list.add(attributeFrom);
      }
    });
    return list;
  }

  /**
   * Finds the first class in a class hierarchy that is annotated with {@link ArtifactClassLoaderRunnerConfig}.
   * <p/>
   * Annotated class is searched by levels, in a Breadth-first search (BFS) way. Starting from the class received as a parameter,
   * first the class is checked and return if annotated. Otherwise interface directly implemented by the class will be added to
   * review and finally its super class.
   * <p/>
   * Implemented interfaces have priority over class hierarchy as base classes can be imported from other modules with different
   * runner configurations.
   *
   * @param testClass class of the test begin executed by the test runner.
   * @return the first class found in the hierarchy that is annotated, null if no class in the hierarchy is annotated.
   */
  public static Class findConfiguredClass(Class<?> testClass) {
    Deque<Class> classesToReview = new LinkedList<>();
    classesToReview.push(testClass);

    while (!classesToReview.isEmpty()) {
      Class currentClass = classesToReview.pop();

      if (currentClass.getDeclaredAnnotation(ArtifactClassLoaderRunnerConfig.class) != null) {
        LOGGER.info("Reading test runner configuration for test '{}' from '{}'", testClass.getName(), currentClass.getName());
        return currentClass;
      }

      addAll(classesToReview, currentClass.getInterfaces());

      if (currentClass.getSuperclass() != null && currentClass.getSuperclass() != Object.class) {
        classesToReview.add(currentClass.getSuperclass());
      }
    }

    return null;
  }

}
