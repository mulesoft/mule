/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.utils;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.sdk.api.annotation.MinMuleVersion;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.mule.sdk.api.util.SdkApiConstants.NON_ENFORCE_MIN_MULE_VERSION_CLASSES;
import static org.slf4j.LoggerFactory.getLogger;

public class JavaParserUtils {

  private static final Logger LOGGER = getLogger(JavaParserUtils.class);
  private static final String firstMuleVersion = "4.1.0";

  private static final Set<String> primitiveTypeNames =
      new HashSet<>(asList("byte", "short", "int", "long", "float", "double", "char", "boolean", "void"));
  private static final List<Class<?>> denyList = NON_ENFORCE_MIN_MULE_VERSION_CLASSES;

  public static String calculateFromClass(Type clazz) {
    return calculateFromClass(clazz, new HashSet<>());
  }

  public static String calculateFromMethod(MethodElement<?> method) {
    return calculateFromMethod(method, new HashSet<>());
  }

  public static String calculateFromParameter(ExtensionParameter parameter) {
    return calculateFromParameter(parameter, new HashSet<>());
  }

  private static String calculateFromClass(Type clazz, Set<String> seenTypes) {
    // TODO W-11938731: fix http extension loading
    if (clazz.getTypeName().startsWith("org.mule.extension.http") || clazz.getTypeName().startsWith("java.")
        || primitiveTypeNames.contains(clazz.getTypeName()) || seenTypes.contains(clazz.getTypeName())) {
      return firstMuleVersion;
    } else {
      seenTypes.add(clazz.getTypeName());
    }
    String calculatedMMV = firstMuleVersion;
    // Look for the annotation at the class level
    Optional<String> classLevelMMV = getMinMuleVersion(clazz);
    if (classLevelMMV.isPresent()) { // We cut the algorithm short (only compare against generics)
      calculatedMMV = maxMMV(calculatedMMV, classLevelMMV.get());
    } else { // We keep calculating from class inspection
      // Parse the superClass
      calculatedMMV =
          maxMMV(calculatedMMV, clazz.getSuperType().map(type -> calculateFromClass(type, seenTypes)).orElse("4.1.0"));
      // Parse the class interfaces
      for (Type type : clazz.getImplementingInterfaces()) {
        calculatedMMV = maxMMV(calculatedMMV, calculateFromClass(type, seenTypes));
      }
      // Parse the class annotations
      for (Type annotation : clazz.getAnnotations()) {
        calculatedMMV = maxMMV(calculatedMMV, calculateFromClass(annotation, seenTypes));
      }
      // Parse the class fields
      for (FieldElement field : clazz.getFields()) {
        calculatedMMV = maxMMV(calculatedMMV, calculateFromParameter(field, seenTypes));
      }
      // Parse the class methods
      for (MethodElement<?> method : clazz.getEnclosingMethods()) {
        calculatedMMV = maxMMV(calculatedMMV, calculateFromMethod(method, seenTypes));
      }
    }
    // Finally we always parse the class generics
    for (TypeGeneric typeGeneric : clazz.getGenerics()) {
      calculatedMMV = maxMMV(calculatedMMV, calculateFromClass(typeGeneric.getConcreteType(), seenTypes));
    }
    return calculatedMMV;
  }

  private static String calculateFromMethod(MethodElement<?> method, Set<String> seenTypes) {
    Optional<String> minMuleVersionAnnotation = getMinMuleVersionFromAnnotation(method);
    if (minMuleVersionAnnotation.isPresent()) {
      return minMuleVersionAnnotation.get();
    }
    String MaxMMV = firstMuleVersion;
    for (Type annotation : method.getAnnotations()) {
      MaxMMV = maxMMV(MaxMMV, calculateFromClass(annotation, seenTypes));
    }
    for (ExtensionParameter parameter : method.getParameters()) {
      MaxMMV = maxMMV(MaxMMV, calculateFromParameter(parameter, seenTypes));
    }
    for (Type exceptionType : method.getExceptionTypes()) {
      MaxMMV = maxMMV(MaxMMV, calculateFromClass(exceptionType, seenTypes));
    }
    return maxMMV(MaxMMV, calculateFromClass(method.getReturnType(), seenTypes));
  }

  private static String calculateFromParameter(ExtensionParameter parameter, Set<String> seenTypes) {
    // Look for the annotation at the parameter level (So far we DON'T allow this, should throw an error here?)
    Optional<String> minMuleVersionAnnotation = getMinMuleVersionFromAnnotation(parameter);
    if (minMuleVersionAnnotation.isPresent()) {
      return minMuleVersionAnnotation.get();
    }
    String maxMMV = firstMuleVersion;
    // Look at parameter annotations and check if they have @MinMuleVersion
    for (Type annotation : parameter.getAnnotations()) {
      maxMMV = maxMMV(maxMMV, calculateFromClass(annotation, seenTypes));
    }
    // Finally analyze the class
    return maxMMV(maxMMV, calculateFromClass(parameter.getType(), seenTypes));
  }

  private static Optional<String> getMinMuleVersion(Type type) {
    for (Class<?> clazz : denyList) {
      if (type.isAssignableFrom(clazz)) {
        return of(firstMuleVersion);
      }
    }
    return getMinMuleVersionFromAnnotation(type);
  }

  private static Optional<String> getMinMuleVersionFromAnnotation(WithAnnotations type) {
    return type.getValueFromAnnotation(MinMuleVersion.class).map(fetcher -> fetcher.getStringValue(MinMuleVersion::value));
  }

  public static String maxMMV(String currentMax, String candidate) {
    if (new MuleVersion(currentMax).atLeast(candidate)) {
      return currentMax;
    }
    return candidate;
  }
}
