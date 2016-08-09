/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class for reading annotations.
 */
public class AnnotationUtils {

  public static boolean methodHasParamAnnotations(Method method) {
    for (int i = 0; i < method.getParameterAnnotations().length; i++) {
      if (method.getParameterAnnotations()[i].length > 0) {
        return true;
      }
    }
    return false;
  }

  public static List<AnnotationMetaData> getParamAnnotationsWithMeta(Method method, Class<? extends Annotation> metaAnnotation) {
    List<AnnotationMetaData> annos = new ArrayList<AnnotationMetaData>();

    for (int i = 0; i < method.getParameterAnnotations().length; i++) {
      for (int j = 0; j < method.getParameterAnnotations()[i].length; j++) {
        Annotation annotation = method.getParameterAnnotations()[i][j];
        if (metaAnnotation == null || annotation.annotationType().isAnnotationPresent(metaAnnotation)) {
          annos.add(new AnnotationMetaData(method.getDeclaringClass(), method, ElementType.PARAMETER, annotation));
        }
      }
    }
    return annos;
  }


  public static List<AnnotationMetaData> getMethodAnnotations(Class<?> c, Class<? extends Annotation> ann) {
    return asList(c.getMethods()).stream().filter(method -> method.isAnnotationPresent(ann))
        .map(method -> new AnnotationMetaData(c, method, METHOD, method.getAnnotation(ann))).collect(toList());
  }

  public static boolean hasAnnotationWithPackage(String packageName, Class<?> clazz) throws IOException {
    for (Annotation anno : clazz.getDeclaredAnnotations()) {
      if (anno.annotationType().getPackage().getName().startsWith(packageName)) {
        return true;
      }
    }

    for (Field field : clazz.getDeclaredFields()) {
      for (Annotation anno : field.getDeclaredAnnotations()) {
        if (anno.annotationType().getPackage().getName().startsWith(packageName)) {
          return true;
        }
      }
    }

    for (Method method : clazz.getDeclaredMethods()) {
      for (Annotation anno : method.getDeclaredAnnotations()) {
        if (anno.annotationType().getPackage().getName().startsWith(packageName)) {
          return true;
        }
      }
    }

    return false;
  }

}
