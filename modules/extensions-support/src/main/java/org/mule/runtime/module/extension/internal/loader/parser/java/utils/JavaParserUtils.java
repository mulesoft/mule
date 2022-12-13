/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.utils;

import org.mule.metadata.api.model.ArrayType;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.sdk.api.annotation.DoNotEnforceMinMuleVersion;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.MinMuleVersion;

import org.slf4j.Logger;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utils class to calculate {@link MinMuleVersion} for SDK Components.
 *
 * @since 4.5
 */
public final class JavaParserUtils {

  private static final Logger LOGGER = getLogger(JavaParserUtils.class);
  private static final MuleVersion SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION =
      new MuleVersion(Extension.class.getAnnotation(MinMuleVersion.class).value());
  public static final MuleVersion FIRST_MULE_VERSION = new MuleVersion("4.1.1");

  private JavaParserUtils() {}

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link ExtensionElement} by looking at the class annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, fields and
   * methods.
   *
   * @param extension the extension to calculate its min mule version
   * @return the minimum mule version of the given extension
   */
  public static MuleVersion calculateExtensionMinMuleVersion(Type extension) {
    MuleVersion calculatedMMV =
        extension.getSuperType().map(JavaParserUtils::calculateExtensionMinMuleVersion).orElse(FIRST_MULE_VERSION);
    if (extension.isAnnotatedWith(Extension.class)) {
      calculatedMMV = max(calculatedMMV, SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION);
    }
    if (!(extension.isAnnotatedWith(Configurations.class)
        || extension.isAnnotatedWith(org.mule.sdk.api.annotation.Configurations.class))) {
      for (FieldElement field : extension.getFields()) {
        calculatedMMV = max(calculatedMMV, calculateFieldMinMuleVersion(field));
      }
      calculatedMMV = max(calculatedMMV, extension.getEnclosingMethods()
          .map(m -> calculateMethodMinMuleVersion(m, FIRST_MULE_VERSION)).reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    }
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(extension);
    MuleVersion finalCalculatedMMV = calculatedMMV;
    return classLevelMMV.map(mmv -> {
      if (mmv.priorTo(finalCalculatedMMV)) {
        LOGGER
            .debug("Calculated Min Mule Version is {} which is greater than the one set at the extension class level {}. Overriding it.",
                   finalCalculatedMMV, mmv);
        return finalCalculatedMMV;
      } else {
        return mmv;
      }
    }).orElse(calculatedMMV);
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link ConfigurationElement} by looking at the class annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, fields,
   * methods and class inheritance.
   *
   * @param config                   the configuration, as a {@link Type}, to calculate its min mule version
   * @param propagatedMinMuleVersion the min mule version propagated from the config's container
   * @return the minimum mule version of the given configuration
   */
  public static MuleVersion calculateConfigMinMuleVersion(Type config, MuleVersion propagatedMinMuleVersion) {
    MuleVersion calculatedMMV = max(propagatedMinMuleVersion, config.getSuperType()
        .map(superType -> calculateConfigMinMuleVersion(superType, propagatedMinMuleVersion)).orElse(FIRST_MULE_VERSION));
    calculatedMMV = max(calculatedMMV, config.getAnnotations().map(JavaParserUtils::getEnforcedMinMuleVersion)
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    for (FieldElement field : config.getFields()) {
      calculatedMMV = max(calculatedMMV, calculateFieldMinMuleVersion(field));
    }
    calculatedMMV = max(calculatedMMV, config.getEnclosingMethods().map(m -> calculateMethodMinMuleVersion(m, FIRST_MULE_VERSION))
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(config);
    MuleVersion finalCalculatedMMV = calculatedMMV;
    return classLevelMMV.map(mmv -> {
      if (mmv.priorTo(finalCalculatedMMV)) {
        LOGGER
            .debug("Calculated Min Mule Version is {} which is greater than the one set at the configuration class level {}. Overriding it.",
                   finalCalculatedMMV, mmv);
        return finalCalculatedMMV;
      } else {
        return mmv;
      }
    }).orElse(calculatedMMV);
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link FunctionElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, parameters
   * and return type.
   *
   * @param function                 the function, as a {@link MethodElement}, to calculate its min mule version
   * @param propagatedMinMuleVersion the min mule version propagated from the function's container
   * @return the minimum mule version of the given function
   */
  public static MuleVersion calculateFunctionMinMuleVersion(MethodElement<?> function, MuleVersion propagatedMinMuleVersion) {
    return calculateMethodMinMuleVersion(function, propagatedMinMuleVersion);
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link OperationElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, parameters
   * and return type.
   *
   * @param operation                the operation, as a {@link MethodElement}, to calculate its min mule version
   * @param propagatedMinMuleVersion the min mule version propagated from the operation's container
   * @return the minimum mule version of the given operation
   */
  public static MuleVersion calculateOperationMinMuleVersion(MethodElement<?> operation,
                                                             OperationContainerElement operationContainer,
                                                             MuleVersion propagatedMinMuleVersion) {
    MuleVersion calculatedMMV = propagatedMinMuleVersion;
    for (ExtensionParameter containerParameter : operationContainer.getParameters()) {
      calculatedMMV = max(calculatedMMV, calculateMethodParameterMinMuleVersion(containerParameter));
    }
    return calculateMethodMinMuleVersion(operation, calculatedMMV);
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link ConnectionProviderElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, fields and
   * class inheritance.
   *
   * @param connectionProvider the connection provider, as a {@link Type}, to calculate its min mule version
   * @return the minimum mule version of the given connection provider
   */
  public static MuleVersion calculateConnectionProviderMinMuleVersion(Type connectionProvider) {
    MuleVersion calculatedMMV = connectionProvider.getSuperType().map(superType -> {
      if (superType.isAssignableTo(ConnectionProvider.class)
          || superType.isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
        return calculateConnectionProviderMinMuleVersion(superType);
      }
      return FIRST_MULE_VERSION;
    }).orElse(FIRST_MULE_VERSION);
    calculatedMMV = max(calculatedMMV, connectionProvider.getImplementingInterfaces()
        .map(JavaParserUtils::calculateInterfacesMinMuleVersion).reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    calculatedMMV = max(calculatedMMV, connectionProvider.getAnnotations().map(JavaParserUtils::getEnforcedMinMuleVersion)
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    for (FieldElement field : connectionProvider.getFields()) {
      calculatedMMV = max(calculatedMMV, calculateFieldMinMuleVersion(field));
    }
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(connectionProvider);
    MuleVersion finalCalculatedMMV = calculatedMMV;
    return classLevelMMV.map(mmv -> {
      if (mmv.priorTo(finalCalculatedMMV)) {
        LOGGER
            .debug("Calculated Min Mule Version is {} which is greater than the one set at the connection provider class level {}. Overriding it.",
                   finalCalculatedMMV, mmv);
        return finalCalculatedMMV;
      } else {
        return mmv;
      }
    }).orElse(calculatedMMV);
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link SourceElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, generics,
   * fields, methods and class inheritance.
   *
   * @param source the connection provider, as a {@link Type}, to calculate its min mule version
   * @return the minimum mule version of the given source
   */
  public static MuleVersion calculateSourceMinMuleVersion(Type source) {
    MuleVersion calculatedMMV = source.getSuperType().map(superType -> {
      if (superType.isSameType(Source.class) || superType.isSameType(org.mule.sdk.api.runtime.source.Source.class)) {
        return getEnforcedMinMuleVersion(superType);
      }
      return calculateSourceMinMuleVersion(superType);
    }).orElse(FIRST_MULE_VERSION);
    if (source.isAssignableTo(Source.class)) {
      calculatedMMV =
          max(calculatedMMV,
              calculateSourceGenericsMinMuleVersion(source.getSuperTypeGenerics(Source.class)));
    }
    if (source.isAssignableTo(org.mule.sdk.api.runtime.source.Source.class)) {
      calculatedMMV =
          max(calculatedMMV,
              calculateSourceGenericsMinMuleVersion(source.getSuperTypeGenerics(org.mule.sdk.api.runtime.source.Source.class)));
    }
    calculatedMMV = max(calculatedMMV, source.getImplementingInterfaces().map(JavaParserUtils::calculateInterfacesMinMuleVersion)
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    calculatedMMV = max(calculatedMMV, source.getAnnotations().map(JavaParserUtils::getEnforcedMinMuleVersion)
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    for (FieldElement field : source.getFields()) {
      calculatedMMV = max(calculatedMMV, calculateFieldMinMuleVersion(field));
    }
    calculatedMMV = max(calculatedMMV, source.getEnclosingMethods().map(m -> calculateMethodMinMuleVersion(m, FIRST_MULE_VERSION))
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(source);
    MuleVersion finalCalculatedMMV = calculatedMMV;
    return classLevelMMV.map(mmv -> {
      if (mmv.priorTo(finalCalculatedMMV)) {
        LOGGER
            .debug("Calculated Min Mule Version is {} which is greater than the one set at the source class level {}. Overriding it.",
                   finalCalculatedMMV, mmv);
        return finalCalculatedMMV;
      } else {
        return mmv;
      }
    }).orElse(calculatedMMV);
  }

  /**
   * For a given sdk component, it checks in its containing element if the component was declared in the given annotation, and, if
   * true, returns the annotation's {@link MinMuleVersion}.
   *
   * @param element          the component to look for
   * @param containerElement the containing element where the component is declared through an annotation
   * @param annotationClass  the annotation class to check
   * @param mapper           a function to extract the values of the annotation class
   * @return the min mule version of the container annotation, if the component was declared there, otherwise a 4.1.1 Mule
   *         Version.
   */
  public static <A extends Annotation> MuleVersion getContainerAnnotationMinMuleVersion(Type containerElement,
                                                                                        Class<A> annotationClass,
                                                                                        Function<A, Class[]> mapper,
                                                                                        Type element) {
    List<Type> sdkConfigurations =
        containerElement.getValueFromAnnotation(annotationClass).map(vf -> vf.getClassArrayValue(mapper)).orElse(emptyList());
    if (sdkConfigurations.stream().anyMatch(element::isSameType)) {
      return new MuleVersion(annotationClass.getAnnotation(MinMuleVersion.class).value());
    }
    return FIRST_MULE_VERSION;
  }

  private static MuleVersion calculateMethodMinMuleVersion(MethodElement<?> method, MuleVersion propagatedMinMuleVersion) {
    MuleVersion calculatedMMV = propagatedMinMuleVersion;
    // Parse the method annotations
    calculatedMMV = max(calculatedMMV, method.getAnnotations().map(JavaParserUtils::getEnforcedMinMuleVersion)
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    for (ExtensionParameter parameter : method.getParameters()) {
      calculatedMMV = max(calculatedMMV, calculateMethodParameterMinMuleVersion(parameter));
    }
    calculatedMMV = max(calculatedMMV, calculateOutputMinMuleVersion(method.getReturnType()));
    Optional<MuleVersion> operationLevelMMV = getMinMuleVersionFromAnnotations(method);
    MuleVersion finalCalculatedMMV = calculatedMMV;
    return operationLevelMMV.map(mmv -> {
      if (mmv.priorTo(finalCalculatedMMV)) {
        LOGGER
            .debug("Calculated Min Mule Version is {} which is greater than the one set at the operation level {}. Overriding it.",
                   finalCalculatedMMV, mmv);
        return finalCalculatedMMV;
      } else {
        return mmv;
      }
    }).orElse(calculatedMMV);
  }

  private static MuleVersion calculateInterfacesMinMuleVersion(Type type) {
    MuleVersion calculatedMMV = getEnforcedMinMuleVersion(type);
    if (belongsToSdkPackages(type.getTypeName())) {
      return calculatedMMV;
    }
    calculatedMMV = max(calculatedMMV, type.getImplementingInterfaces().map(JavaParserUtils::calculateInterfacesMinMuleVersion)
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    return calculatedMMV;
  }

  private static MuleVersion calculateSourceGenericsMinMuleVersion(List<Type> sourceGenerics) {
    Type outputType = sourceGenerics.get(0);
    Type attributesType = sourceGenerics.get(1);
    return max(getEnforcedMinMuleVersion(attributesType), calculateOutputMinMuleVersion(outputType));
  }

  private static MuleVersion calculateOutputMinMuleVersion(Type outputType) {
    MuleVersion calculatedMMV = FIRST_MULE_VERSION;
    if (outputType.asMetadataType() instanceof ArrayType) {
      for (TypeGeneric typeGeneric : outputType.getGenerics()) {
        calculatedMMV = max(calculatedMMV, calculateOutputMinMuleVersion(typeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(Result.class) || outputType.isSameType(org.mule.sdk.api.runtime.operation.Result.class)) {
      calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(outputType));
      for (TypeGeneric resultTypeGeneric : outputType.getGenerics()) {
        calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(resultTypeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(PagingProvider.class)
        || outputType.isSameType(org.mule.sdk.api.runtime.streaming.PagingProvider.class)) {
      calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(outputType));
      calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(outputType.getGenerics().get(1).getConcreteType()));
    } else {
      calculatedMMV = getEnforcedMinMuleVersion(outputType);
    }
    return calculatedMMV;
  }

  private static MuleVersion calculateFieldMinMuleVersion(ExtensionParameter field) {
    MuleVersion calculatedMMV = getMinMuleVersionFromAnnotations(field).orElse(FIRST_MULE_VERSION);
    Type parameterType = field.getType();
    for (Type annotation : field.getAnnotations().collect(toList())) {
      if (annotation.isSameType(Inject.class) && !parameterType.isSameType(Optional.class)) {
        // Parse injected classes but exclude Optionals (such as ForwardCompatibilityHelper)
        calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(parameterType));
      }
      if (annotation.isSameType(Parameter.class)
          || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.Parameter.class)) {
        calculatedMMV = max(calculatedMMV, calculateParameterContainerMinMuleVersion(parameterType));
      }
      if (annotation.isSameType(ParameterGroup.class)
          || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.ParameterGroup.class)) {
        calculatedMMV = max(calculatedMMV, calculateParameterContainerMinMuleVersion(parameterType));
      }
      if (annotation.isSameType(Connection.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Connection.class)) {
        // Sources inject the ConnectionProvider instead of the connection
        if (parameterType.isAssignableTo(ConnectionProvider.class)
            || parameterType.isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
          calculatedMMV = max(calculatedMMV, calculateConnectionProviderMinMuleVersion(parameterType));
        }
      }
      if (annotation.isSameType(Config.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Config.class)) {
        calculatedMMV =
            max(calculatedMMV, calculateConfigMinMuleVersion(parameterType, getEnforcedMinMuleVersion(annotation)));
      }
      calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(annotation));
    }
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(parameterType.getTypeName())) {
      calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(parameterType));
    }
    return calculatedMMV;
  }

  private static MuleVersion calculateMethodParameterMinMuleVersion(ExtensionParameter methodParameter) {
    MuleVersion calculatedMMV = FIRST_MULE_VERSION;
    for (Type annotation : methodParameter.getAnnotations().collect(toList())) {
      if (annotation.isSameType(ParameterGroup.class)
          || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.ParameterGroup.class)) {
        calculatedMMV = max(calculatedMMV, calculateParameterContainerMinMuleVersion(methodParameter.getType()));
      }
      if (annotation.isSameType(Config.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Config.class)) {
        calculatedMMV =
            max(calculatedMMV,
                calculateConfigMinMuleVersion(methodParameter.getType(), getEnforcedMinMuleVersion(annotation)));
      }
      calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(annotation));
    }
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(methodParameter.getType().getTypeName())) {
      calculatedMMV = max(calculatedMMV, getEnforcedMinMuleVersion(methodParameter.getType()));
    }
    return calculatedMMV;
  }

  private static MuleVersion calculateParameterContainerMinMuleVersion(Type containerType) {
    Optional<MuleVersion> minMuleVersionAnnotation = getMinMuleVersionFromAnnotations(containerType);
    if (minMuleVersionAnnotation.isPresent()) {
      return minMuleVersionAnnotation.get();
    }
    MuleVersion calculatedMMV = FIRST_MULE_VERSION;
    calculatedMMV = max(calculatedMMV, containerType.getAnnotations().map(JavaParserUtils::getEnforcedMinMuleVersion)
        .reduce(FIRST_MULE_VERSION, JavaParserUtils::max));
    for (FieldElement field : containerType.getFields()) {
      calculatedMMV = max(calculatedMMV, calculateFieldMinMuleVersion(field));
    }
    return calculatedMMV;
  }

  private static MuleVersion getEnforcedMinMuleVersion(Type type) {
    if (type.isAnnotatedWith(DoNotEnforceMinMuleVersion.class)) {
      return FIRST_MULE_VERSION;
    }
    return getMinMuleVersionFromAnnotations(type).orElse(FIRST_MULE_VERSION);
  }

  private static Optional<MuleVersion> getMinMuleVersionFromAnnotations(WithAnnotations type) {
    return type.getValueFromAnnotation(MinMuleVersion.class)
        .map(fetcher -> new MuleVersion(fetcher.getStringValue(MinMuleVersion::value)));
  }

  private static MuleVersion max(MuleVersion currentMax, MuleVersion candidate) {
    if (currentMax.atLeast(candidate)) {
      return currentMax;
    }
    return candidate;
  }

  private static boolean belongsToSdkPackages(String fullyQualifiedName) {
    return belongsToExtensionsApiPackages(fullyQualifiedName) || belongsToSdkApiPackages(fullyQualifiedName);
  }

  private static boolean belongsToExtensionsApiPackages(String fullyQualifiedName) {
    return fullyQualifiedName.startsWith("org.mule.runtime.extension.api");
  }

  private static boolean belongsToSdkApiPackages(String fullyQualifiedName) {
    return fullyQualifiedName.startsWith("org.mule.sdk.api");
  }
}
