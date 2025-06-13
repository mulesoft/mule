/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.utils;

import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion.FIRST_MULE_VERSION;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.declaration.fluent.HasMinMuleVersionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.WithMinMuleVersionDeclaration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.loader.parser.MinMuleVersionParser;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
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

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;

import jakarta.inject.Inject;

/**
 * Utils class to create {@link ResolvedMinMuleVersion}s from {@link Type}s.
 *
 * @since 4.5
 */
public final class MinMuleVersionUtils {

  private static final Logger LOGGER = getLogger(MinMuleVersionUtils.class);

  private static final MuleVersion SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION =
      new MuleVersion(Extension.class.getAnnotation(MinMuleVersion.class).value());

  private MinMuleVersionUtils() {}

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link ExtensionElement} by looking at the class annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, fields and
   * methods.
   *
   * @param extension the extension to calculate its min mule version
   * @return the minimum mule version of the given extension
   */
  public static ResolvedMinMuleVersion resolveExtensionMinMuleVersion(Type extension) {
    if (belongsToJavaPackages(extension.getTypeName())) {
      return resolveToDefaultMMV("Java type", extension.getTypeName());
    }
    ResolvedMinMuleVersion extensionMMV;
    if (extension.isAnnotatedWith(Extension.class)) {
      extensionMMV = new ResolvedMinMuleVersion(extension.getName(), SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION,
                                                "Extension " + extension.getName() + " has min mule version "
                                                    + SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION
                                                    + " because it if annotated with the new sdk api @Extension.");
    } else {
      extensionMMV = resolveToDefaultMMV("Extension", extension.getName());
    }
    Optional<ResolvedMinMuleVersion> superExtensionMMV =
        extension.getSuperType().map(MinMuleVersionUtils::resolveExtensionMinMuleVersion);
    superExtensionMMV.ifPresent(resolvedMMV -> extensionMMV
        .updateIfHigherMMV(resolvedMMV, getReasonSuperClass("Extension", extension.getName(), resolvedMMV)));
    if (!(extension.isAnnotatedWith(Configurations.class)
        || extension.isAnnotatedWith(org.mule.sdk.api.annotation.Configurations.class))) {
      Optional<ResolvedMinMuleVersion> fieldMMV =
          extension.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
              .reduce(MinMuleVersionUtils::max);
      fieldMMV.ifPresent(resolvedMMV -> extensionMMV
          .updateIfHigherMMV(resolvedMMV, getReasonField("Extension", extension.getName(), resolvedMMV)));
      Optional<ResolvedMinMuleVersion> methodMMV =
          extension.getEnclosingMethods().map(MinMuleVersionUtils::calculateMethodMinMuleVersion)
              .reduce(MinMuleVersionUtils::max);
      methodMMV.ifPresent(resolvedMMV -> extensionMMV
          .updateIfHigherMMV(resolvedMMV, getReasonMethod("Extension", extension.getName(), resolvedMMV)));
    }
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(extension);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(extensionMMV.getMinMuleVersion())) {
        return new ResolvedMinMuleVersion(extension.getName(), extensionMMV.getMinMuleVersion(),
                                          getReasonOverride(extensionMMV.getMinMuleVersion().toString(), "extension class",
                                                            classLevelMMV.get().toString(), extensionMMV.getReason()));
      } else {
        return new ResolvedMinMuleVersion(extension.getName(), classLevelMMV.get(),
                                          getReasonClassLevelMMV("Extension", extension.getName(), classLevelMMV.get()));
      }
    }
    return extensionMMV;
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
  public static ResolvedMinMuleVersion resolveConfigurationMinMuleVersion(Type config, MuleVersion propagatedMinMuleVersion) {
    if (belongsToJavaPackages(config.getTypeName())) {
      return resolveToDefaultMMV("Java type", config.getTypeName());
    }
    ResolvedMinMuleVersion configMMV = max(resolveToDefaultMMV("Configuration", config.getName()),
                                           new ResolvedMinMuleVersion(config.getName(), propagatedMinMuleVersion, "Configuration "
                                               + config.getName() + " has min mule version " + propagatedMinMuleVersion
                                               + " because it was propagated from the annotation (either @Configurations or @Config) used to reference this configuration."));
    Optional<Type> superType = config.getSuperType();
    if (superType.isPresent()) {
      ResolvedMinMuleVersion superConfigMMV = resolveConfigurationMinMuleVersion(superType.get(), propagatedMinMuleVersion);
      configMMV.updateIfHigherMMV(superConfigMMV, getReasonSuperClass("Configuration", config.getName(), superConfigMMV));
    }
    Optional<ResolvedMinMuleVersion> annotationMMV =
        config.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationMMV.ifPresent(resolvedMMV -> configMMV
        .updateIfHigherMMV(resolvedMMV, getReasonAnnotated("Configuration", config.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> fieldMMV =
        config.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
            .reduce(MinMuleVersionUtils::max);
    fieldMMV.ifPresent(resolvedMMV -> configMMV
        .updateIfHigherMMV(resolvedMMV, getReasonField("Configuration", config.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> methodMMV =
        config.getEnclosingMethods().map(MinMuleVersionUtils::calculateMethodMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    methodMMV.ifPresent(resolvedMMV -> configMMV
        .updateIfHigherMMV(resolvedMMV, getReasonMethod("Configuration", config.getName(), resolvedMMV)));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(config);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(configMMV.getMinMuleVersion())) {
        return new ResolvedMinMuleVersion(config.getName(), configMMV.getMinMuleVersion(),
                                          getReasonOverride(configMMV.getMinMuleVersion().toString(), "configuration class",
                                                            classLevelMMV.get().toString(), configMMV.getReason()));
      } else {
        return new ResolvedMinMuleVersion(config.getName(), classLevelMMV.get(),
                                          getReasonClassLevelMMV("Configuration", config.getName(), classLevelMMV.get()));
      }
    }
    return configMMV;
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
  public static ResolvedMinMuleVersion resolveFunctionMinMuleVersion(MethodElement<?> function,
                                                                     MuleVersion propagatedMinMuleVersion) {
    ResolvedMinMuleVersion resolvedMMV =
        max(resolveToDefaultMMV("Function", function.getName()),
            new ResolvedMinMuleVersion(function.getName(), propagatedMinMuleVersion, "Function " + function.getName()
                + " has min mule version " + propagatedMinMuleVersion
                + " because it was propagated from the @Functions annotation at the extension class used to add the function."));
    resolvedMMV = max(resolvedMMV, calculateMethodMinMuleVersion(function));
    return resolvedMMV;
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
  public static ResolvedMinMuleVersion resolveOperationMinMuleVersion(MethodElement<?> operation,
                                                                      OperationContainerElement operationContainer,
                                                                      MuleVersion propagatedMinMuleVersion) {
    ResolvedMinMuleVersion operationMMV = resolveToDefaultMMV("Operation", operation.getName());
    operationMMV = max(operationMMV, new ResolvedMinMuleVersion(operation.getName(), propagatedMinMuleVersion, "Operation "
        + operation.getName() + " has min mule version " + propagatedMinMuleVersion
        + " because it was propagated from the @Operations annotation at the extension class used to add the operation's container "
        + operationContainer.getName() + "."));
    Optional<ResolvedMinMuleVersion> parameterMMV = operationContainer.getParameters().stream()
        .map(MinMuleVersionUtils::calculateMethodParameterMinMuleVersion)
        .reduce(MinMuleVersionUtils::max);
    if (parameterMMV.isPresent()) {
      operationMMV.updateIfHigherMMV(parameterMMV.get(),
                                     getReasonParameter("Operation", operation.getName(), parameterMMV.get()));
    }
    operationMMV = max(operationMMV, calculateMethodMinMuleVersion(operation));
    return operationMMV;
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link ConnectionProviderElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, fields and
   * class inheritance.
   *
   * @param connectionProvider the connection provider, as a {@link Type}, to calculate its min mule version
   * @return the minimum mule version of the given connection provider
   */
  public static ResolvedMinMuleVersion resolveConnectionProviderMinMuleVersion(Type connectionProvider) {
    if (belongsToJavaPackages(connectionProvider.getTypeName())) {
      return resolveToDefaultMMV("Java type", connectionProvider.getTypeName());
    }
    ResolvedMinMuleVersion connectionProviderMMV =
        resolveToDefaultMMV("Connection Provider", connectionProvider.getName());
    Optional<Type> superType = connectionProvider.getSuperType();
    if (superType.isPresent()) {
      if (superType.get().isAssignableTo(ConnectionProvider.class)
          || superType.get().isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
        ResolvedMinMuleVersion superTypeMMV = resolveConnectionProviderMinMuleVersion(superType.get());
        connectionProviderMMV
            .updateIfHigherMMV(superTypeMMV,
                               getReasonSuperClass("Connection Provider", connectionProvider.getName(), superTypeMMV));
      }
    }
    Optional<ResolvedMinMuleVersion> interfaceMMV = connectionProvider.getImplementingInterfaces()
        .map(MinMuleVersionUtils::calculateInterfaceMinMuleVersion).reduce(MinMuleVersionUtils::max);
    interfaceMMV.ifPresent(resolvedMMV -> connectionProviderMMV
        .updateIfHigherMMV(resolvedMMV, getReasonInterface("Connection Provider", connectionProvider.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> annotationMMV =
        connectionProvider.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationMMV.ifPresent(resolvedMMV -> connectionProviderMMV
        .updateIfHigherMMV(resolvedMMV, getReasonAnnotated("Connection Provider", connectionProvider.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> fieldMMV =
        connectionProvider.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
            .reduce(MinMuleVersionUtils::max);
    fieldMMV.ifPresent(resolvedMMV -> connectionProviderMMV
        .updateIfHigherMMV(resolvedMMV, getReasonField("Connection Provider", connectionProvider.getName(), resolvedMMV)));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(connectionProvider);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo((connectionProviderMMV.getMinMuleVersion()))) {
        return new ResolvedMinMuleVersion(connectionProvider.getName(), connectionProviderMMV.getMinMuleVersion(),
                                          getReasonOverride(connectionProviderMMV.getMinMuleVersion().toString(),
                                                            "connection provider class", classLevelMMV.get().toString(),
                                                            connectionProviderMMV.getReason()));
      } else {
        return new ResolvedMinMuleVersion(connectionProvider.getName(), classLevelMMV
            .get(), getReasonClassLevelMMV("Connection Provider", connectionProvider.getName(), classLevelMMV.get()));
      }
    }
    return connectionProviderMMV;
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link SourceElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, generics,
   * fields, methods and class inheritance.
   *
   * @param source the connection provider, as a {@link Type}, to calculate its min mule version
   * @return the minimum mule version of the given source
   */
  public static ResolvedMinMuleVersion resolveSourceMinMuleVersion(SourceElement source) {
    ResolvedMinMuleVersion genericsMMV = calculateSourceGenericsMinMuleVersion(source.getSuperClassGenerics());
    ResolvedMinMuleVersion sourceMMV = calculateSourceMinMuleVersion(source);
    sourceMMV.updateIfHigherMMV(genericsMMV, "Source " + source.getName() + " has min mule version "
        + genericsMMV.getMinMuleVersion() + " because it has a generic of type " + genericsMMV.getName() + ".");
    return sourceMMV;
  }

  private static ResolvedMinMuleVersion calculateSourceMinMuleVersion(Type source) {
    if (belongsToJavaPackages(source.getTypeName())) {
      return resolveToDefaultMMV("Java type", source.getTypeName());
    }
    ResolvedMinMuleVersion sourceMMV = resolveToDefaultMMV("Source", source.getName());
    Optional<Type> superType = source.getSuperType();
    if (superType.isPresent()) {
      ResolvedMinMuleVersion superTypeMMV;
      if (superType.get().isSameType(Source.class) || superType.get().isSameType(org.mule.sdk.api.runtime.source.Source.class)) {
        superTypeMMV = getEnforcedMinMuleVersion(superType.get());
      } else {
        superTypeMMV = calculateSourceMinMuleVersion(superType.get());
      }
      sourceMMV.updateIfHigherMMV(superTypeMMV, getReasonSuperClass("Source", source.getName(), superTypeMMV));
    }
    Optional<ResolvedMinMuleVersion> interfaceMMV =
        source.getImplementingInterfaces().map(MinMuleVersionUtils::calculateInterfaceMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    interfaceMMV.ifPresent(resolvedMMV -> sourceMMV
        .updateIfHigherMMV(resolvedMMV, getReasonInterface("Source", source.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> annotationMMV =
        source.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationMMV.ifPresent(resolvedMMV -> sourceMMV
        .updateIfHigherMMV(resolvedMMV, getReasonAnnotated("Source", source.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> fieldMMV =
        source.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
            .reduce(MinMuleVersionUtils::max);
    fieldMMV.ifPresent(resolvedMMV -> sourceMMV.updateIfHigherMMV(resolvedMMV,
                                                                  getReasonField("Source", source.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> methodMMV =
        source.getEnclosingMethods().map(MinMuleVersionUtils::calculateMethodMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    methodMMV.ifPresent(resolvedMMV -> sourceMMV.updateIfHigherMMV(resolvedMMV,
                                                                   getReasonMethod("Source", source.getName(), resolvedMMV)));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(source);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(sourceMMV.getMinMuleVersion())) {
        return new ResolvedMinMuleVersion(source.getName(), sourceMMV.getMinMuleVersion(),
                                          getReasonOverride(sourceMMV.getMinMuleVersion().toString(), "source class",
                                                            classLevelMMV.get().toString(), sourceMMV.getReason()));
      } else {
        return new ResolvedMinMuleVersion(source.getName(), classLevelMMV.get(),
                                          getReasonClassLevelMMV("Source", source.getName(), classLevelMMV.get()));
      }
    }
    return sourceMMV;
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

  private static ResolvedMinMuleVersion calculateMethodMinMuleVersion(MethodElement<?> method) {
    ResolvedMinMuleVersion methodMMV = resolveToDefaultMMV("Method", method.getName());
    Optional<ResolvedMinMuleVersion> annotationMMV =
        method.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationMMV.ifPresent(resolvedMMV -> methodMMV
        .updateIfHigherMMV(resolvedMMV, getReasonAnnotated("Method", method.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> parameterMMV =
        method.getParameters().stream().map(MinMuleVersionUtils::calculateMethodParameterMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    parameterMMV.ifPresent(resolvedMMV -> methodMMV
        .updateIfHigherMMV(resolvedMMV, getReasonParameter("Method", method.getName(), resolvedMMV)));
    ResolvedMinMuleVersion outputMMV = calculateOutputMinMuleVersion(method.getReturnType());
    methodMMV.updateIfHigherMMV(outputMMV, "Method " + method.getName() + " has min mule version " + outputMMV.getMinMuleVersion()
        + " because of its output type " + outputMMV.getName() + ".");
    Optional<MuleVersion> operationLevelMMV = getMinMuleVersionFromAnnotations(method);
    if (operationLevelMMV.isPresent()) {
      if (operationLevelMMV.get().priorTo(methodMMV.getMinMuleVersion())) {
        return new ResolvedMinMuleVersion(method.getName(), methodMMV.getMinMuleVersion(),
                                          getReasonOverride(methodMMV.getMinMuleVersion().toString(), "method",
                                                            operationLevelMMV.get().toString(), methodMMV.getReason()));
      } else {
        return new ResolvedMinMuleVersion(method.getName(), operationLevelMMV.get(), "Method " + method.getName()
            + " has min mule version " + operationLevelMMV.get()
            + " because it is the one set at the method level through the @MinMuleVersion annotation.");
      }
    }
    return methodMMV;
  }

  private static ResolvedMinMuleVersion calculateInterfaceMinMuleVersion(Type interfaceType) {
    ResolvedMinMuleVersion interfaceMMV = getEnforcedMinMuleVersion(interfaceType);
    if (belongsToSdkPackages(interfaceType.getTypeName())) {
      return interfaceMMV;
    }
    Optional<ResolvedMinMuleVersion> superInterface = interfaceType.getImplementingInterfaces()
        .map(MinMuleVersionUtils::calculateInterfaceMinMuleVersion).reduce(MinMuleVersionUtils::max);
    superInterface.ifPresent(resolvedMMV -> interfaceMMV
        .updateIfHigherMMV(resolvedMMV, getReasonInterface("Interface", interfaceType.getName(), resolvedMMV)));
    return interfaceMMV;
  }

  private static ResolvedMinMuleVersion calculateSourceGenericsMinMuleVersion(List<Type> sourceGenerics) {
    Type outputType = sourceGenerics.get(0);
    Type attributesType = sourceGenerics.get(1);
    return max(getEnforcedMinMuleVersion(attributesType), calculateOutputMinMuleVersion(outputType));
  }

  private static ResolvedMinMuleVersion calculateOutputMinMuleVersion(Type outputType) {
    ResolvedMinMuleVersion resolvedMMV = resolveToDefaultMMV("Output type", outputType.getName());
    if (outputType.isArray()) {
      resolvedMMV = max(resolvedMMV, calculateOutputMinMuleVersion(outputType.getArrayComponentType().get()));
    } else if (outputType.isAssignableTo(Iterable.class) || outputType.isAssignableTo(Iterator.class)) {
      for (TypeGeneric typeGeneric : outputType.getGenerics()) {
        resolvedMMV = max(resolvedMMV, calculateOutputMinMuleVersion(typeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(Result.class) || outputType.isSameType(org.mule.sdk.api.runtime.operation.Result.class)) {
      resolvedMMV = max(resolvedMMV, getEnforcedMinMuleVersion(outputType));
      for (TypeGeneric MMVTypeGeneric : outputType.getGenerics()) {
        resolvedMMV = max(resolvedMMV, getEnforcedMinMuleVersion(MMVTypeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(PagingProvider.class)
        || outputType.isSameType(org.mule.sdk.api.runtime.streaming.PagingProvider.class)) {
      resolvedMMV = max(resolvedMMV, getEnforcedMinMuleVersion(outputType));
      resolvedMMV =
          max(resolvedMMV, getEnforcedMinMuleVersion(outputType.getGenerics().get(1).getConcreteType()));
    } else {
      resolvedMMV = getEnforcedMinMuleVersion(outputType);
    }
    return resolvedMMV;
  }

  private static ResolvedMinMuleVersion calculateFieldMinMuleVersion(ExtensionParameter field,
                                                                     Set<String> seenTypesForRecursionControl) {
    Type parameterType = field.getType();
    if (seenTypesForRecursionControl.contains(parameterType.getTypeName())) {
      // If it is a recursive type we only look at its annotations and cut the recursion.
      ResolvedMinMuleVersion recursiveFieldMMV = resolveToDefaultMMV("Field", field.getName());
      Optional<ResolvedMinMuleVersion> annotationMMV =
          field.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
              .reduce(MinMuleVersionUtils::max);
      annotationMMV.ifPresent(resolvedMMV -> recursiveFieldMMV
          .updateIfHigherMMV(resolvedMMV, getReasonAnnotated("Field", field.getName(), resolvedMMV)));
      return recursiveFieldMMV;
    } else {
      seenTypesForRecursionControl.add(parameterType.getTypeName());
    }
    ResolvedMinMuleVersion fieldMinMuleVersion = getMinMuleVersionFromAnnotations(field)
        .map(mmv -> new ResolvedMinMuleVersion(field.getName(), mmv,
                                               "Field " + field.getName() + " has min mule version " + mmv
                                                   + " because it is annotated with @MinMuleVersion."))
        .orElse(resolveToDefaultMMV("Field", field.getName()));
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(parameterType.getTypeName())) {
      ResolvedMinMuleVersion typeMMV = getEnforcedMinMuleVersion(parameterType);
      fieldMinMuleVersion.updateIfHigherMMV(typeMMV, getReasonType("Field", field.getName(), typeMMV));
    }
    for (Type annotation : field.getAnnotations().collect(toList())) {
      calculateFieldAnnotationMinMuleVersion(field, seenTypesForRecursionControl, parameterType, fieldMinMuleVersion, annotation);
    }
    return fieldMinMuleVersion;
  }

  private static void calculateFieldAnnotationMinMuleVersion(ExtensionParameter field, Set<String> seenTypesForRecursionControl,
                                                             Type parameterType, ResolvedMinMuleVersion fieldMinMuleVersion,
                                                             Type annotation) {
    if ((annotation.isSameType(Inject.class)
        // Still need to support javax.inject for the time being...
        || annotation.isSameType(javax.inject.Inject.class))
        && !parameterType.isSameType(Optional.class)) {
      // Parse injected classes but exclude Optionals (such as ForwardCompatibilityHelper)
      ResolvedMinMuleVersion typeMMV = getEnforcedMinMuleVersion(parameterType);
      fieldMinMuleVersion.updateIfHigherMMV(typeMMV, getReasonType("Field", field.getName(), typeMMV));
    }
    if (isParameterOrParameterGroup(annotation)) {
      ResolvedMinMuleVersion parameterContainerMMV =
          calculateParameterTypeMinMuleVersion(parameterType, seenTypesForRecursionControl);
      fieldMinMuleVersion.updateIfHigherMMV(parameterContainerMMV,
                                            getReasonFieldType(field.getName(),
                                                               parameterContainerMMV.getMinMuleVersion().toString(),
                                                               "parameter", parameterContainerMMV.getName()));
    }
    if (annotation.isSameType(Connection.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Connection.class)) {
      // Sources inject the ConnectionProvider instead of the connection
      if (parameterType.isAssignableTo(ConnectionProvider.class)
          || parameterType.isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
        ResolvedMinMuleVersion connectionProviderMMV = resolveConnectionProviderMinMuleVersion(parameterType);
        fieldMinMuleVersion.updateIfHigherMMV(connectionProviderMMV,
                                              getReasonFieldType(field.getName(),
                                                                 connectionProviderMMV.getMinMuleVersion().toString(),
                                                                 "connection provider", connectionProviderMMV.getName()));
      }
    }
    if (annotation.isSameType(Config.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Config.class)) {
      ResolvedMinMuleVersion configurationMMV =
          resolveConfigurationMinMuleVersion(parameterType, getEnforcedMinMuleVersion(annotation).getMinMuleVersion());
      fieldMinMuleVersion.updateIfHigherMMV(configurationMMV,
                                            getReasonFieldType(field.getName(), configurationMMV.getMinMuleVersion().toString(),
                                                               "configuration", configurationMMV.getName()));
    }
    ResolvedMinMuleVersion annotationMMV = getEnforcedMinMuleVersion(annotation);
    fieldMinMuleVersion.updateIfHigherMMV(annotationMMV, getReasonAnnotated("Field", field.getName(), annotationMMV));
  }

  private static ResolvedMinMuleVersion calculateMethodParameterMinMuleVersion(ExtensionParameter methodParameter) {
    ResolvedMinMuleVersion methodParameterMMV = resolveToDefaultMMV("Parameter", methodParameter.getName());
    Optional<ResolvedMinMuleVersion> configAnnotationMMV =
        methodParameter.getAnnotations()
            .filter(a -> a.isAssignableTo(Config.class) || a.isAssignableTo(org.mule.sdk.api.annotation.param.Config.class))
            .findFirst().map(MinMuleVersionUtils::getEnforcedMinMuleVersion);
    if (configAnnotationMMV.isPresent()) {
      ResolvedMinMuleVersion configMMV =
          resolveConfigurationMinMuleVersion(methodParameter.getType(), configAnnotationMMV.get().getMinMuleVersion());
      methodParameterMMV
          .updateIfHigherMMV(configMMV, "Parameter " + methodParameter.getName() + " has min mule version "
              + configMMV.getMinMuleVersion() + " because it references a config of type " + configMMV.getName() + ".");
    }
    Optional<ResolvedMinMuleVersion> annotationMMV =
        methodParameter.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion).reduce(MinMuleVersionUtils::max);
    annotationMMV.ifPresent(resolvedMMV -> methodParameterMMV
        .updateIfHigherMMV(resolvedMMV, getReasonAnnotated("Parameter", methodParameter.getName(), resolvedMMV)));
    ResolvedMinMuleVersion parameterTypeMMV;
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(methodParameter.getType().getTypeName())) {
      parameterTypeMMV = getEnforcedMinMuleVersion(methodParameter.getType());
    } else { // The parameter is of custom type
      if (methodParameter.getAnnotations()
          .noneMatch(a -> a.isSameType(Connection.class) || a.isSameType(org.mule.sdk.api.annotation.param.Connection.class))) {
        parameterTypeMMV = calculateParameterTypeMinMuleVersion(methodParameter.getType(), new HashSet<>());
      } else {
        parameterTypeMMV = resolveToDefaultMMV("Type", methodParameter.getType().getName());
      }
    }
    methodParameterMMV.updateIfHigherMMV(parameterTypeMMV,
                                         getReasonType("Parameter", methodParameter.getName(), parameterTypeMMV));
    return methodParameterMMV;
  }

  private static ResolvedMinMuleVersion calculateParameterTypeMinMuleVersion(Type parameterType,
                                                                             Set<String> seenTypesForRecursionControl) {
    Optional<MuleVersion> minMuleVersionAnnotation = getMinMuleVersionFromAnnotations(parameterType);
    if (minMuleVersionAnnotation.isPresent()) {
      String reason = "Type " + parameterType.getName() + " has min mule version " + minMuleVersionAnnotation.get()
          + " because it is annotated with @MinMuleVersion.";
      return new ResolvedMinMuleVersion(parameterType.getName(), minMuleVersionAnnotation.get(), reason);
    }
    ResolvedMinMuleVersion parameterMMV = resolveToDefaultMMV("Type", parameterType.getName());

    Optional<ResolvedMinMuleVersion> fieldMMV = empty();
    // TODO W-14749120 - properly handle provided types that are missing
    try {
      fieldMMV = parameterType.getFields().stream()
          .filter(MinMuleVersionUtils::isAnnotatedWithParameterOrParameterGroup)
          .map(f -> calculateFieldMinMuleVersion(f, seenTypesForRecursionControl)).reduce(MinMuleVersionUtils::max);
    } catch (Throwable t) {
      if (isLinkageError(t)) {
        LOGGER
            .warn(format("Skipping MMV calculation for fields of type '%s', at least one of the fields' types couldn't be loaded",
                         parameterType.getName()));
      } else {
        throw t;
      }
    }

    fieldMMV.ifPresent(resolvedMMV -> parameterMMV
        .updateIfHigherMMV(resolvedMMV, getReasonField("Type", parameterType.getName(), resolvedMMV)));
    Optional<ResolvedMinMuleVersion> annotationMMV =
        parameterType.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationMMV.ifPresent(resolvedMMV -> parameterMMV
        .updateIfHigherMMV(resolvedMMV, getReasonAnnotated("Type", parameterType.getName(), resolvedMMV)));
    return parameterMMV;
  }

  public static void declarerWithMmv(HasMinMuleVersionDeclarer<?> declarer, MinMuleVersionParser resolvedMMV) {
    declarer.withMinMuleVersion(resolvedMMV.getMinMuleVersion());
    LOGGER.debug(resolvedMMV.getReason());
  }

  public static void declarationWithMmv(WithMinMuleVersionDeclaration declaration, MinMuleVersionParser resolvedMMV) {
    declaration.withMinMuleVersion(resolvedMMV.getMinMuleVersion());
    LOGGER.debug(resolvedMMV.getReason());
  }

  private static boolean isLinkageError(Throwable t) {
    Set<Throwable> seen = new HashSet<>();
    while (t != null && seen.add(t)) {
      if (t instanceof LinkageError) {
        return true;
      }

      t = t.getCause();
    }

    return false;
  }

  private static ResolvedMinMuleVersion getEnforcedMinMuleVersion(Type type) {
    if (type.isAnnotatedWith(DoNotEnforceMinMuleVersion.class)) {
      return new ResolvedMinMuleVersion(type.getName(), FIRST_MULE_VERSION,
                                        type.getName() + " has the default base min mule version " + FIRST_MULE_VERSION
                                            + " because it is annotated with @DoNotEnforceMinMuleVersion.");
    }
    Optional<MuleVersion> mmv = getMinMuleVersionFromAnnotations(type);
    return mmv
        .map(muleVersion -> new ResolvedMinMuleVersion(type.getName(), muleVersion,
                                                       type.getName() + " was introduced in Mule " + muleVersion + "."))
        .orElseGet(() -> resolveToDefaultMMV("Type", type.getName()));
  }

  private static Optional<MuleVersion> getMinMuleVersionFromAnnotations(WithAnnotations type) {
    return type.getValueFromAnnotation(MinMuleVersion.class)
        .map(fetcher -> new MuleVersion(fetcher.getStringValue(MinMuleVersion::value)));
  }

  private static ResolvedMinMuleVersion max(ResolvedMinMuleVersion currentMax, ResolvedMinMuleVersion candidate) {
    if (currentMax.getMinMuleVersion().atLeast(candidate.getMinMuleVersion())) {
      return currentMax;
    }
    return candidate;
  }

  private static ResolvedMinMuleVersion resolveToDefaultMMV(String componentDescription, String componentName) {
    return new ResolvedMinMuleVersion(componentName, FIRST_MULE_VERSION, componentDescription + " " + componentName
        + " has min mule version " + FIRST_MULE_VERSION + " because it is the default value.");
  }

  private static boolean isParameterOrParameterGroup(Type annotation) {
    return (annotation.isSameType(Parameter.class)
        || annotation.isSameType(org.mule.sdk.api.annotation.param.Parameter.class)
        || annotation.isSameType(ParameterGroup.class)
        || annotation.isSameType(org.mule.sdk.api.annotation.param.ParameterGroup.class));
  }

  private static boolean isAnnotatedWithParameterOrParameterGroup(WithAnnotations type) {
    return (type.isAnnotatedWith(Parameter.class)
        || type.isAnnotatedWith(org.mule.sdk.api.annotation.param.Parameter.class)
        || type.isAnnotatedWith(ParameterGroup.class)
        || type.isAnnotatedWith(org.mule.sdk.api.annotation.param.ParameterGroup.class));
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

  private static boolean belongsToJavaPackages(String fullyQualifiedName) {
    return fullyQualifiedName.startsWith("java.");
  }

  private static String getReasonAnnotated(String component, String name, ResolvedMinMuleVersion annotationMMV) {
    return component + " " + name + " has min mule version " + annotationMMV.getMinMuleVersion()
        + " because it is annotated with "
        + annotationMMV.getName() + ".";
  }

  private static String getReasonSuperClass(String component, String name, ResolvedMinMuleVersion superClassMMV) {
    return component + " " + name + " has min mule version " + superClassMMV.getMinMuleVersion() + " because of its super class "
        + superClassMMV.getName() + ".";
  }

  private static String getReasonField(String component, String name, ResolvedMinMuleVersion fieldMMV) {
    return component + " " + name + " has min mule version " + fieldMMV.getMinMuleVersion() + " because of its field "
        + fieldMMV.getName() + ".";
  }


  private static String getReasonFieldType(String fieldName, String mmv, String componentType, String type) {
    return "Field " + fieldName + " has min mule version " + mmv + " because it is a " + componentType + " of type " + type + ".";
  }

  private static String getReasonInterface(String component, String name, ResolvedMinMuleVersion interfaceMMV) {
    return component + " " + name + " has min mule version " + interfaceMMV.getMinMuleVersion()
        + " because it implements interface " + interfaceMMV.getName() + ".";
  }

  private static String getReasonParameter(String component, String name, ResolvedMinMuleVersion parameterMMV) {
    return component + " " + name + " has min mule version " + parameterMMV.getMinMuleVersion() + " because of its parameter "
        + parameterMMV.getName() + ".";
  }

  private static String getReasonType(String component, String name, ResolvedMinMuleVersion typeMMV) {
    return component + " " + name + " has min mule version " + typeMMV.getMinMuleVersion() + " because it is of type "
        + typeMMV.getName() + ".";
  }

  private static String getReasonMethod(String component, String name, ResolvedMinMuleVersion methodMMV) {
    return component + " " + name + " has min mule version " + methodMMV.getMinMuleVersion() + " because of its method "
        + methodMMV.getName() + ".";
  }

  private static String getReasonClassLevelMMV(String component, String name, MuleVersion mmv) {
    return component + " " + name + " has min mule version " + mmv
        + " because it is the one set at the class level through the @MinMuleVersion annotation.";
  }

  private static String getReasonOverride(String resolvedMMV, String componentType, String classLevelMMV, String reason) {
    return "Calculated Min Mule Version is " + resolvedMMV + " which is greater than the one set at the " + componentType
        + " level " + classLevelMMV + ". Overriding it. " + reason;
  }
}
