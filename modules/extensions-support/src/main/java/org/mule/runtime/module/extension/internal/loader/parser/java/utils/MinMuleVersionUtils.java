/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.utils;

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

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.MuleVersion.FIRST_MULE_VERSION;

/**
 * Utils class to create {@link MinMuleVersionResult}s from {@link Type}s.
 *
 * @since 4.5
 */
public final class MinMuleVersionUtils {

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
  public static MinMuleVersionResult getExtensionResult(Type extension) {
    if (belongsToJavaPackages(extension.getTypeName())) {
      return createResultWithDefaultMMV("Java type", extension.getTypeName());
    }
    MinMuleVersionResult extensionResult;
    if (extension.isAnnotatedWith(Extension.class)) {
      extensionResult = new MinMuleVersionResult(extension.getName(), SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION,
                                                 format("Extension %s has min mule version %s because it if annotated with the new sdk api @Extension.",
                                                        extension.getName(), SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION));
    } else {
      extensionResult = createResultWithDefaultMMV("Extension", extension.getName());
    }
    Optional<MinMuleVersionResult> superExtension =
        extension.getSuperType().map(MinMuleVersionUtils::getExtensionResult);
    superExtension.ifPresent(minMuleVersionResult -> extensionResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                       format("Extension %s has min mule version %s because of its super class %s.",
                                                                                              extension.getName(),
                                                                                              minMuleVersionResult
                                                                                                  .getMinMuleVersion(),
                                                                                              minMuleVersionResult.getName())));
    if (!(extension.isAnnotatedWith(Configurations.class)
        || extension.isAnnotatedWith(org.mule.sdk.api.annotation.Configurations.class))) {
      Optional<MinMuleVersionResult> fieldResult =
          extension.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
              .reduce(MinMuleVersionUtils::max);
      fieldResult.ifPresent(minMuleVersionResult -> extensionResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                      format("Extension %s has min mule version %s because of its field %s.",
                                                                                             extension.getName(),
                                                                                             minMuleVersionResult
                                                                                                 .getMinMuleVersion(),
                                                                                             minMuleVersionResult.getName())));
      Optional<MinMuleVersionResult> methodResult =
          extension.getEnclosingMethods().map(MinMuleVersionUtils::calculateMethodMinMuleVersion)
              .reduce(MinMuleVersionUtils::max);
      methodResult.ifPresent(minMuleVersionResult -> extensionResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                       format("Extension %s has min mule version %s because of its method %s.",
                                                                                              extension.getName(),
                                                                                              minMuleVersionResult
                                                                                                  .getMinMuleVersion(),
                                                                                              minMuleVersionResult.getName())));
    }
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(extension);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(extensionResult.getMinMuleVersion())) {
        return new MinMuleVersionResult(extension.getName(), extensionResult.getMinMuleVersion(),
                                        format("Calculated Min Mule Version is %s which is greater than the one set at the extension class level %s. Overriding it. %s",
                                               extensionResult.getMinMuleVersion(), classLevelMMV.get(),
                                               extensionResult.getReason()));
      } else {
        return new MinMuleVersionResult(extension.getName(), classLevelMMV.get(),
                                        format("Extension %s has min mule version %s because it is the one set at the class level through the @MinMuleVersion annotation.",
                                               extension.getName(), classLevelMMV.get()));
      }
    }
    return extensionResult;
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
  public static MinMuleVersionResult getConfigurationResult(Type config, MuleVersion propagatedMinMuleVersion) {
    if (belongsToJavaPackages(config.getTypeName())) {
      return createResultWithDefaultMMV("Java type", config.getTypeName());
    }
    MinMuleVersionResult configResult = max(createResultWithDefaultMMV("Configuration", config.getName()),
                                            new MinMuleVersionResult(config.getName(), propagatedMinMuleVersion,
                                                                     format("Configuration %s has min mule version %s because it was propagated from the annotation (either @Configurations or @Config) used to reference this configuration.",
                                                                            config.getName(), propagatedMinMuleVersion)));
    Optional<Type> superType = config.getSuperType();
    if (superType.isPresent()) {
      MinMuleVersionResult superConfigResult = getConfigurationResult(superType.get(), propagatedMinMuleVersion);
      configResult.updateIfHigherMMV(superConfigResult,
                                     format("Configuration %s has min mule version %s due to its super class %s.",
                                            config.getName(),
                                            superConfigResult.getMinMuleVersion(), superConfigResult.getName()));
    }
    Optional<MinMuleVersionResult> annotationResult =
        config.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationResult.ifPresent(minMuleVersionResult -> configResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                      format("Configuration %s has min mule version %s because it is annotated with %s.",
                                                                                             config.getName(),
                                                                                             minMuleVersionResult
                                                                                                 .getMinMuleVersion(),
                                                                                             minMuleVersionResult.getName())));
    Optional<MinMuleVersionResult> fieldResult =
        config.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
            .reduce(MinMuleVersionUtils::max);
    fieldResult.ifPresent(minMuleVersionResult -> configResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                 format("Configuration %s has min mule version %s because of its field %s.",
                                                                                        config.getName(),
                                                                                        minMuleVersionResult.getMinMuleVersion(),
                                                                                        minMuleVersionResult.getName())));
    Optional<MinMuleVersionResult> methodResult =
        config.getEnclosingMethods().map(MinMuleVersionUtils::calculateMethodMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    methodResult.ifPresent(minMuleVersionResult -> configResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                  format("Configuration %s has min mule version %s because of its method %s",
                                                                                         config.getName(),
                                                                                         minMuleVersionResult.getMinMuleVersion(),
                                                                                         minMuleVersionResult.getName())));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(config);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(configResult.getMinMuleVersion())) {
        return new MinMuleVersionResult(config.getName(), configResult.getMinMuleVersion(),
                                        format("Calculated Min Mule Version is %s which is greater than the one set at the configuration class level %s. Overriding it. %s",
                                               configResult.getMinMuleVersion(), classLevelMMV.get(), configResult.getReason()));
      } else {
        return new MinMuleVersionResult(config.getName(), classLevelMMV.get(),
                                        format("Configuration %s has min mule version %s because it is the one set at the class level through the @MinMuleVersion annotation.",
                                               config.getName(), classLevelMMV.get()));
      }
    }
    return configResult;
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
  public static MinMuleVersionResult getFunctionResult(MethodElement<?> function, MuleVersion propagatedMinMuleVersion) {
    MinMuleVersionResult minMuleVersionResult =
        max(createResultWithDefaultMMV("Function", function.getName()),
            new MinMuleVersionResult(function.getName(), propagatedMinMuleVersion,
                                     format("Function %s has min mule version %s because it was propagated from the @Functions annotation at the extension class used to add the function.",
                                            function.getName(), propagatedMinMuleVersion)));
    minMuleVersionResult = max(minMuleVersionResult, calculateMethodMinMuleVersion(function));
    return minMuleVersionResult;
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
  public static MinMuleVersionResult getOperationResult(MethodElement<?> operation,
                                                        OperationContainerElement operationContainer,
                                                        MuleVersion propagatedMinMuleVersion) {
    MinMuleVersionResult operationResult = createResultWithDefaultMMV("Operation", operation.getName());
    operationResult = max(operationResult, new MinMuleVersionResult(operation.getName(), propagatedMinMuleVersion,
                                                                    format("Operation %s has min mule version %s because it was propagated from the @Operations annotation at the extension class used to add the operation's container %s.",
                                                                           operation.getName(), propagatedMinMuleVersion,
                                                                           operationContainer.getName())));
    Optional<MinMuleVersionResult> parameterResult = operationContainer.getParameters().stream()
        .map(MinMuleVersionUtils::calculateMethodParameterMinMuleVersion)
        .reduce(MinMuleVersionUtils::max);
    if (parameterResult.isPresent()) {
      operationResult.updateIfHigherMMV(parameterResult.get(),
                                        format("Operation %s has min mule version %s because of its parameter %s.",
                                               operation.getName(),
                                               parameterResult.get().getMinMuleVersion(),
                                               parameterResult.get().getName()));
    }
    operationResult = max(operationResult, calculateMethodMinMuleVersion(operation));
    return operationResult;
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link ConnectionProviderElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, fields and
   * class inheritance.
   *
   * @param connectionProvider the connection provider, as a {@link Type}, to calculate its min mule version
   * @return the minimum mule version of the given connection provider
   */
  public static MinMuleVersionResult getConnectionProviderResult(Type connectionProvider) {
    if (belongsToJavaPackages(connectionProvider.getTypeName())) {
      return createResultWithDefaultMMV("Java type", connectionProvider.getTypeName());
    }
    MinMuleVersionResult connectionProviderResult =
        createResultWithDefaultMMV("Connection Provider", connectionProvider.getName());
    Optional<Type> superType = connectionProvider.getSuperType();
    if (superType.isPresent()) {
      if (superType.get().isAssignableTo(ConnectionProvider.class)
          || superType.get().isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
        MinMuleVersionResult superTypeResult = getConnectionProviderResult(superType.get());
        connectionProviderResult.updateIfHigherMMV(superTypeResult,
                                                   format("Connection Provider %s has min mule version %s because of its super class %s.",
                                                          connectionProvider.getName(), superTypeResult.getMinMuleVersion(),
                                                          superTypeResult.getName()));
      }
    }
    Optional<MinMuleVersionResult> interfaceResult = connectionProvider.getImplementingInterfaces()
        .map(MinMuleVersionUtils::calculateInterfaceMinMuleVersion).reduce(MinMuleVersionUtils::max);
    interfaceResult.ifPresent(comp -> connectionProviderResult.updateIfHigherMMV(comp,
                                                                                 format("Connection Provider %s has min mule version %s because of its interface %s.",
                                                                                        connectionProvider.getName(),
                                                                                        comp.getMinMuleVersion(),
                                                                                        comp.getName())));
    Optional<MinMuleVersionResult> annotationResult =
        connectionProvider.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationResult.ifPresent(comp -> connectionProviderResult.updateIfHigherMMV(comp,
                                                                                  format("Connection Provider %s has min mule version %s because it is annotated with %s.",
                                                                                         connectionProvider.getName(),
                                                                                         comp.getMinMuleVersion(),
                                                                                         comp.getName())));
    Optional<MinMuleVersionResult> fieldResult =
        connectionProvider.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
            .reduce(MinMuleVersionUtils::max);
    fieldResult.ifPresent(comp -> connectionProviderResult.updateIfHigherMMV(comp,
                                                                             format("Connection Provider %s has min mule version %s because of its field %s.",
                                                                                    connectionProvider.getName(),
                                                                                    comp.getMinMuleVersion(),
                                                                                    comp.getName())));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(connectionProvider);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo((connectionProviderResult.getMinMuleVersion()))) {
        return new MinMuleVersionResult(connectionProvider.getName(), connectionProviderResult.getMinMuleVersion(),
                                        format("Calculated Min Mule Version is %s which is greater than the one set at the connection provider class level %s. Overriding it. %s",
                                               connectionProviderResult.getMinMuleVersion(), classLevelMMV.get(),
                                               connectionProviderResult.getReason()));
      } else {
        return new MinMuleVersionResult(connectionProvider.getName(), classLevelMMV.get(),
                                        format("Connection Provider %s has min mule version %s because it is the one set at the class level.",
                                               connectionProvider.getName(), classLevelMMV.get()));
      }
    }
    return connectionProviderResult;
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link SourceElement} by looking at the annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, generics,
   * fields, methods and class inheritance.
   *
   * @param source the connection provider, as a {@link Type}, to calculate its min mule version
   * @return the minimum mule version of the given source
   */
  public static MinMuleVersionResult getSourceResult(SourceElement source) {
    MinMuleVersionResult genericsResult = calculateSourceGenericsMinMuleVersion(source.getSuperClassGenerics());
    MinMuleVersionResult sourceResult = calculateSourceResult(source);
    sourceResult.updateIfHigherMMV(genericsResult,
                                   format("Source %s has min mule version %s because it has a generic of type %s.",
                                          source.getName(), genericsResult.getMinMuleVersion(), genericsResult.getName()));
    return sourceResult;
  }

  public static MinMuleVersionResult calculateSourceResult(Type source) {
    if (belongsToJavaPackages(source.getTypeName())) {
      return createResultWithDefaultMMV("Java type", source.getTypeName());
    }
    MinMuleVersionResult sourceResult = createResultWithDefaultMMV("Source", source.getName());
    Optional<Type> superType = source.getSuperType();
    if (superType.isPresent()) {
      MinMuleVersionResult superTypeResult;
      if (superType.get().isSameType(Source.class) || superType.get().isSameType(org.mule.sdk.api.runtime.source.Source.class)) {
        superTypeResult = getEnforcedMinMuleVersion(superType.get());
      } else {
        superTypeResult = calculateSourceResult(superType.get());
      }
      sourceResult.updateIfHigherMMV(superTypeResult,
                                     format("Source %s has min mule version %s because of its super class %s.",
                                            source.getName(),
                                            superTypeResult.getMinMuleVersion(), superTypeResult.getName()));
    }
    Optional<MinMuleVersionResult> interfaceResult =
        source.getImplementingInterfaces().map(MinMuleVersionUtils::calculateInterfaceMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    interfaceResult.ifPresent(minMuleVersionResult -> sourceResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                     format("Source %s has min mule version %s because it implements interface %s.",
                                                                                            source.getName(),
                                                                                            minMuleVersionResult
                                                                                                .getMinMuleVersion(),
                                                                                            minMuleVersionResult.getName())));
    Optional<MinMuleVersionResult> annotationResult =
        source.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationResult.ifPresent(minMuleVersionResult -> sourceResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                      format("Source %s has min mule version %s because it is annotated with %s.",
                                                                                             source.getName(),
                                                                                             minMuleVersionResult
                                                                                                 .getMinMuleVersion(),
                                                                                             minMuleVersionResult.getName())));
    Optional<MinMuleVersionResult> fieldResult =
        source.getFields().stream().map(f -> calculateFieldMinMuleVersion(f, new HashSet<>()))
            .reduce(MinMuleVersionUtils::max);
    fieldResult.ifPresent(minMuleVersionResult -> sourceResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                 format("Source %s has min mule version %s because of its field %s.",
                                                                                        source.getName(),
                                                                                        minMuleVersionResult.getMinMuleVersion(),
                                                                                        minMuleVersionResult.getName())));
    Optional<MinMuleVersionResult> methodResult =
        source.getEnclosingMethods().map(MinMuleVersionUtils::calculateMethodMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    methodResult.ifPresent(minMuleVersionResult -> sourceResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                  format("Source %s has min mule version %s because of its method %s.",
                                                                                         source.getName(),
                                                                                         minMuleVersionResult.getMinMuleVersion(),
                                                                                         minMuleVersionResult.getName())));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(source);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(sourceResult.getMinMuleVersion())) {
        return new MinMuleVersionResult(source.getName(), sourceResult.getMinMuleVersion(),
                                        format("Calculated Min Mule Version is %s which is greater than the one set at the source class level %s. Overriding it. %s",
                                               sourceResult.getMinMuleVersion(), classLevelMMV.get(), sourceResult.getReason()));
      } else {
        return new MinMuleVersionResult(source.getName(), classLevelMMV.get(),
                                        format("Source %s has min mule version %s because it is the one set at the class level.",
                                               source.getName(), classLevelMMV.get()));
      }
    }
    return sourceResult;
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

  private static MinMuleVersionResult calculateMethodMinMuleVersion(MethodElement<?> method) {
    MinMuleVersionResult methodResult = createResultWithDefaultMMV("Method", method.getName());
    Optional<MinMuleVersionResult> annotationResult =
        method.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationResult.ifPresent(minMuleVersionResult -> methodResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                      format("Method %s has min mule version %s because it is annotated with %s.",
                                                                                             method.getName(),
                                                                                             minMuleVersionResult
                                                                                                 .getMinMuleVersion(),
                                                                                             minMuleVersionResult.getName())));
    Optional<MinMuleVersionResult> parameterResult =
        method.getParameters().stream().map(MinMuleVersionUtils::calculateMethodParameterMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    parameterResult.ifPresent(minMuleVersionResult -> methodResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                     format("Method %s has min mule version %s because of its parameter %s.",
                                                                                            method.getName(),
                                                                                            minMuleVersionResult
                                                                                                .getMinMuleVersion(),
                                                                                            minMuleVersionResult.getName())));
    MinMuleVersionResult outputResult = calculateOutputMinMuleVersion(method.getReturnType());
    methodResult.updateIfHigherMMV(outputResult,
                                   format("Method %s has min mule version %s because of its output type %s.", method.getName(),
                                          outputResult.getMinMuleVersion(), outputResult.getName()));
    Optional<MuleVersion> operationLevelMMV = getMinMuleVersionFromAnnotations(method);
    if (operationLevelMMV.isPresent()) {
      if (operationLevelMMV.get().priorTo(methodResult.getMinMuleVersion())) {
        return new MinMuleVersionResult(method.getName(), methodResult.getMinMuleVersion(),
                                        format("Calculated Min Mule Version is %s which is greater than the one set at the method level %s. Overriding it. %s",
                                               methodResult.getMinMuleVersion(), operationLevelMMV.get(),
                                               methodResult.getReason()));
      } else {
        return new MinMuleVersionResult(method.getName(), operationLevelMMV.get(),
                                        format("Method %s has min mule version %s because it is the one set at the method level through the @MinMuleVersion annotation.",
                                               method.getName(), operationLevelMMV.get()));
      }
    }
    return methodResult;
  }

  private static MinMuleVersionResult calculateInterfaceMinMuleVersion(Type interfaceType) {
    MinMuleVersionResult interfaceResult = getEnforcedMinMuleVersion(interfaceType);
    if (belongsToSdkPackages(interfaceType.getTypeName())) {
      return interfaceResult;
    }
    Optional<MinMuleVersionResult> superInterface = interfaceType.getImplementingInterfaces()
        .map(MinMuleVersionUtils::calculateInterfaceMinMuleVersion).reduce(MinMuleVersionUtils::max);
    superInterface.ifPresent(minMuleVersionResult -> interfaceResult.updateIfHigherMMV(minMuleVersionResult,
                                                                                       format("Interface %s has min mule version %s because it implements %s.",
                                                                                              interfaceType.getName(),
                                                                                              minMuleVersionResult
                                                                                                  .getMinMuleVersion(),
                                                                                              minMuleVersionResult
                                                                                                  .getName())));
    return interfaceResult;
  }

  private static MinMuleVersionResult calculateSourceGenericsMinMuleVersion(List<Type> sourceGenerics) {
    Type outputType = sourceGenerics.get(0);
    Type attributesType = sourceGenerics.get(1);
    return max(getEnforcedMinMuleVersion(attributesType), calculateOutputMinMuleVersion(outputType));
  }

  private static MinMuleVersionResult calculateOutputMinMuleVersion(Type outputType) {
    MinMuleVersionResult minMuleVersionResult = createResultWithDefaultMMV("Output type", outputType.getName());
    if (outputType.isArray()) {
      minMuleVersionResult = max(minMuleVersionResult, calculateOutputMinMuleVersion(outputType.getArrayComponentType().get()));
    } else if (outputType.isAssignableTo(Iterable.class) || outputType.isAssignableTo(Iterator.class)) {
      for (TypeGeneric typeGeneric : outputType.getGenerics()) {
        minMuleVersionResult = max(minMuleVersionResult, calculateOutputMinMuleVersion(typeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(Result.class) || outputType.isSameType(org.mule.sdk.api.runtime.operation.Result.class)) {
      minMuleVersionResult = max(minMuleVersionResult, getEnforcedMinMuleVersion(outputType));
      for (TypeGeneric resultTypeGeneric : outputType.getGenerics()) {
        minMuleVersionResult = max(minMuleVersionResult, getEnforcedMinMuleVersion(resultTypeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(PagingProvider.class)
        || outputType.isSameType(org.mule.sdk.api.runtime.streaming.PagingProvider.class)) {
      minMuleVersionResult = max(minMuleVersionResult, getEnforcedMinMuleVersion(outputType));
      minMuleVersionResult =
          max(minMuleVersionResult, getEnforcedMinMuleVersion(outputType.getGenerics().get(1).getConcreteType()));
    } else {
      minMuleVersionResult = getEnforcedMinMuleVersion(outputType);
    }
    return minMuleVersionResult;
  }

  private static MinMuleVersionResult calculateFieldMinMuleVersion(ExtensionParameter field,
                                                                   Set<String> seenTypesForRecursionControl) {
    Type parameterType = field.getType();
    if (seenTypesForRecursionControl.contains(parameterType.getTypeName())) {
      // If it is a recursive type we only look at its annotations and cut the recursion.
      MinMuleVersionResult recursiveFieldResult = createResultWithDefaultMMV("Field", field.getName());
      Optional<MinMuleVersionResult> annotationResult =
          field.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
              .reduce(MinMuleVersionUtils::max);
      annotationResult.ifPresent(comp -> recursiveFieldResult.updateIfHigherMMV(comp,
                                                                                format("Field %s has min mule version %s because it is annotated with %s.",
                                                                                       field.getName(),
                                                                                       comp.getMinMuleVersion(),
                                                                                       comp.getName())));
      return recursiveFieldResult;
    } else {
      seenTypesForRecursionControl.add(parameterType.getTypeName());
    }
    MinMuleVersionResult minMuleVersionResult = getMinMuleVersionFromAnnotations(field)
        .map(mmv -> new MinMuleVersionResult(field.getName(), mmv,
                                             format("Field %s has min mule version %s because it is annotated with @MinMuleVersion.",
                                                    field.getName(), mmv)))
        .orElse(createResultWithDefaultMMV("Field", field.getName()));
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(parameterType.getTypeName())) {
      MinMuleVersionResult typeResult = getEnforcedMinMuleVersion(parameterType);
      minMuleVersionResult.updateIfHigherMMV(typeResult,
                                             format("Field %s has min mule version %s because it is of type %s.", field.getName(),
                                                    typeResult.getMinMuleVersion(), typeResult.getName()));
    }
    for (Type annotation : field.getAnnotations().collect(toList())) {
      if (annotation.isSameType(Inject.class) && !parameterType.isSameType(Optional.class)) {
        // Parse injected classes but exclude Optionals (such as ForwardCompatibilityHelper)
        MinMuleVersionResult typeResult = getEnforcedMinMuleVersion(parameterType);
        minMuleVersionResult.updateIfHigherMMV(typeResult,
                                               format("Field %s has min mule version %s because it is of type %s.",
                                                      field.getName(),
                                                      typeResult.getMinMuleVersion(), typeResult.getName()));
      }
      if (annotation.isSameType(Parameter.class)
          || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.Parameter.class)) {
        MinMuleVersionResult parameterContainerResult =
            calculateParameterTypeMinMuleVersion(parameterType, seenTypesForRecursionControl);
        minMuleVersionResult.updateIfHigherMMV(parameterContainerResult,
                                               format("Field %s has min mule version %s because it is a parameter of type %s.",
                                                      field.getName(), parameterContainerResult.getMinMuleVersion(),
                                                      parameterContainerResult.getName()));
      }
      if (annotation.isSameType(ParameterGroup.class)
          || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.ParameterGroup.class)) {
        MinMuleVersionResult parameterGroupResult =
            calculateParameterTypeMinMuleVersion(parameterType, seenTypesForRecursionControl);
        minMuleVersionResult.updateIfHigherMMV(parameterGroupResult,
                                               format("Field %s has min mule version %s because it is a parameter group of type %s.",
                                                      field.getName(), parameterGroupResult.getMinMuleVersion(),
                                                      parameterGroupResult.getName()));
      }
      if (annotation.isSameType(Connection.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Connection.class)) {
        // Sources inject the ConnectionProvider instead of the connection
        if (parameterType.isAssignableTo(ConnectionProvider.class)
            || parameterType.isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
          MinMuleVersionResult connectionProviderResult = getConnectionProviderResult(parameterType);
          minMuleVersionResult.updateIfHigherMMV(connectionProviderResult,
                                                 format("Field %s has min mule version %s because it is a connection provider of type %s.",
                                                        field.getName(), connectionProviderResult.getMinMuleVersion(),
                                                        connectionProviderResult.getName()));
        }
      }
      if (annotation.isSameType(Config.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Config.class)) {
        MinMuleVersionResult configurationResult =
            getConfigurationResult(parameterType, getEnforcedMinMuleVersion(annotation).getMinMuleVersion());
        minMuleVersionResult.updateIfHigherMMV(configurationResult,
                                               format("Field %s has min mule version %s because it is a configuration of type %s.",
                                                      field.getName(), configurationResult.getMinMuleVersion(),
                                                      configurationResult.getName()));
      }
      MinMuleVersionResult annotationResult = getEnforcedMinMuleVersion(annotation);
      minMuleVersionResult.updateIfHigherMMV(annotationResult,
                                             format("Field %s has min mule version %s because it is annotated with %s.",
                                                    field.getName(),
                                                    annotationResult.getMinMuleVersion(), annotationResult.getName()));
    }
    return minMuleVersionResult;
  }

  private static MinMuleVersionResult calculateMethodParameterMinMuleVersion(ExtensionParameter methodParameter) {
    MinMuleVersionResult methodParameterResult = createResultWithDefaultMMV("Parameter", methodParameter.getName());
    if (methodParameter.isAnnotatedWith(Config.class)) {
      MinMuleVersionResult configResult =
          getConfigurationResult(methodParameter.getType(),
                                 new MuleVersion(Config.class.getAnnotation(MinMuleVersion.class).value()));
      methodParameterResult
          .updateIfHigherMMV(configResult,
                             format("Parameter %s has min mule version %s because it references a config of type %s.",
                                    methodParameter.getName(), configResult.getMinMuleVersion(), configResult.getName()));
    } else if (methodParameter.isAnnotatedWith(org.mule.sdk.api.annotation.param.Config.class)) {
      MinMuleVersionResult configResult =
          getConfigurationResult(methodParameter.getType(), new MuleVersion(org.mule.sdk.api.annotation.param.Config.class
              .getAnnotation(MinMuleVersion.class).value()));
      methodParameterResult
          .updateIfHigherMMV(configResult,
                             format("Parameter %s has min mule version %s because it references a config of type %s.",
                                    methodParameter.getName(), configResult.getMinMuleVersion(), configResult.getName()));
    }
    Optional<MinMuleVersionResult> annotationResult =
        methodParameter.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion).reduce(MinMuleVersionUtils::max);
    annotationResult.ifPresent(minMuleVersionResult -> methodParameterResult
        .updateIfHigherMMV(minMuleVersionResult,
                           format("Parameter %s has min mule version %s because it is annotated with %s.",
                                  methodParameter.getName(), minMuleVersionResult.getMinMuleVersion(),
                                  minMuleVersionResult.getName())));

    MinMuleVersionResult parameterTypeResult = calculateParameterTypeMinMuleVersion(methodParameter.getType(), new HashSet<>());
    methodParameterResult.updateIfHigherMMV(parameterTypeResult,
                                            format("Parameter %s has min mule version %s because it is of type %s.",
                                                   methodParameter.getName(), parameterTypeResult.getMinMuleVersion(),
                                                   parameterTypeResult.getName()));
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(methodParameter.getType().getTypeName())) {
      MinMuleVersionResult typeResult = getEnforcedMinMuleVersion(methodParameter.getType());
      methodParameterResult.updateIfHigherMMV(typeResult,
                                              format("Parameter %s has min mule version %s because it is of type %s.",
                                                     methodParameter.getName(), typeResult.getMinMuleVersion(),
                                                     typeResult.getName()));
    }
    return methodParameterResult;
  }

  private static MinMuleVersionResult calculateParameterTypeMinMuleVersion(Type parameterType,
                                                                           Set<String> seenTypesForRecursionControl) {
    Optional<MuleVersion> minMuleVersionAnnotation = getMinMuleVersionFromAnnotations(parameterType);
    if (minMuleVersionAnnotation.isPresent()) {
      String reason = format("Type %s has min mule version %s because it is annotated with @MinMuleVersion.",
                             parameterType.getName(), minMuleVersionAnnotation.get());
      return new MinMuleVersionResult(parameterType.getName(), minMuleVersionAnnotation.get(), reason);
    }
    MinMuleVersionResult minMuleVersionResult = createResultWithDefaultMMV("Type", parameterType.getName());
    Optional<MinMuleVersionResult> fieldResult = parameterType.getFields().stream()
        .filter(f -> f.isAnnotatedWith(Parameter.class) || f.isAnnotatedWith(org.mule.sdk.api.annotation.param.Parameter.class))
        .map(f -> calculateFieldMinMuleVersion(f, seenTypesForRecursionControl)).reduce(MinMuleVersionUtils::max);
    fieldResult.ifPresent(muleVersionResult -> minMuleVersionResult
        .updateIfHigherMMV(muleVersionResult,
                           format("Type %s has min mule version %s because of its field %s.", parameterType.getName(),
                                  muleVersionResult.getMinMuleVersion(), muleVersionResult.getName())));
    Optional<MinMuleVersionResult> annotationResult =
        parameterType.getAnnotations().map(MinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(MinMuleVersionUtils::max);
    annotationResult.ifPresent(muleVersionResult -> minMuleVersionResult
        .updateIfHigherMMV(muleVersionResult,
                           format("Type %s has min mule version %s because it is annotated with %s.", parameterType.getName(),
                                  muleVersionResult.getMinMuleVersion(), muleVersionResult.getName())));
    return minMuleVersionResult;
  }

  private static MinMuleVersionResult getEnforcedMinMuleVersion(Type type) {
    if (type.isAnnotatedWith(DoNotEnforceMinMuleVersion.class)) {
      return new MinMuleVersionResult(type.getName(), FIRST_MULE_VERSION,
                                      format("%s has the default base min mule version %s because it is annotated with @DoNotEnforceMinMuleVersion.",
                                             type.getName(), FIRST_MULE_VERSION));
    }
    Optional<MuleVersion> mmv = getMinMuleVersionFromAnnotations(type);
    return mmv
        .map(muleVersion -> new MinMuleVersionResult(type.getName(), muleVersion,
                                                     format("%s has min mule version %s because it is annotated with @MinMuleVersion.",
                                                            type.getName(), muleVersion)))
        .orElseGet(() -> createResultWithDefaultMMV("Type", type.getName()));
  }

  private static Optional<MuleVersion> getMinMuleVersionFromAnnotations(WithAnnotations type) {
    return type.getValueFromAnnotation(MinMuleVersion.class)
        .map(fetcher -> new MuleVersion(fetcher.getStringValue(MinMuleVersion::value)));
  }

  private static MinMuleVersionResult max(MinMuleVersionResult currentMax, MinMuleVersionResult candidate) {
    if (currentMax.getMinMuleVersion().atLeast(candidate.getMinMuleVersion())) {
      return currentMax;
    }
    return candidate;
  }

  private static MinMuleVersionResult createResultWithDefaultMMV(String componentDescription, String componentName) {
    return new MinMuleVersionResult(componentName, FIRST_MULE_VERSION,
                                    format("%s %s has min mule version %s because it is the default value.",
                                           componentDescription, componentName, FIRST_MULE_VERSION));
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
}
