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

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utils class to calculate {@link MinMuleVersion} for SDK Components.
 *
 * @since 4.5
 */
public final class SdkComponentsMinMuleVersionUtils {

  private static final Logger LOGGER = getLogger(SdkComponentsMinMuleVersionUtils.class);
  private static final MuleVersion SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION =
          new MuleVersion(Extension.class.getAnnotation(MinMuleVersion.class).value());
  public static final MuleVersion FIRST_MULE_VERSION = new MuleVersion("4.1.1");

  private SdkComponentsMinMuleVersionUtils() {}

  public static class ComponentInformation {

    private String componentName;
    private MuleVersion minMuleVersion;
    private String reason;
    private ComponentInformation innerComponent = null;

    public ComponentInformation(String componentName, MuleVersion minMuleVersion, String reason) {
      this.componentName = componentName;
      this.minMuleVersion = minMuleVersion;
      this.reason = reason;
    }

    public void setMinMuleVersion(MuleVersion minMuleVersion) {
      this.minMuleVersion = minMuleVersion;
    }

    public MuleVersion getMinMuleVersion() {
      return minMuleVersion;
    }

    public String getReason() {
      if (innerComponent != null) {
        return reason.concat(" ").concat(innerComponent.getReason());
      }
      return reason;
    }

    public String getName() {
      return componentName;
    }

    public void updateIfHigherMMV(ComponentInformation candidate, String reason) {
      if (!(this.minMuleVersion.atLeast(candidate.getMinMuleVersion()))) {
        this.reason = reason;
        this.minMuleVersion = candidate.getMinMuleVersion();
        this.innerComponent = candidate;
      }
    }
  }

  /**
   * Calculates the minimum {@link MuleVersion} for a given {@link ExtensionElement} by looking at the class annotation
   * {@link MinMuleVersion} (if present) and at the new SDK API components it interacts with through its annotations, fields and
   * methods.
   *
   * @param extension the extension to calculate its min mule version
   * @return the minimum mule version of the given extension
   */
  public static MuleVersion calculateExtensionMinMuleVersion(Type extension) {
    ComponentInformation componentInformation = createExtensionComponent(extension);
    LOGGER.info(componentInformation.getReason());
    return componentInformation.getMinMuleVersion();
  }

  private static ComponentInformation createExtensionComponent(Type extension) {
    ComponentInformation extensionComponent;
    if (extension.isAnnotatedWith(Extension.class)) {
      extensionComponent = new ComponentInformation(extension.getName(), SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION,
              format("Extension %s has min mule version %s because it if annotated with the new sdk api @Extension.",
                      extension.getName(), SDK_EXTENSION_ANNOTATION_MIN_MULE_VERSION));
    } else {
      extensionComponent = createComponentWithDefaultMMV("Extension", extension.getName());
    }
    Optional<ComponentInformation> superExtension =
        extension.getSuperType().map(SdkComponentsMinMuleVersionUtils::createExtensionComponent);
    if (superExtension.isPresent()) {
      extensionComponent.updateIfHigherMMV(superExtension.get(),
                                           format("Extension %s has min mule version %s because of its super class %s.",
                                                  extension.getName(),
                                                  superExtension.get().getMinMuleVersion(), superExtension.get().getName()));
    }
    if (!(extension.isAnnotatedWith(Configurations.class)
            || extension.isAnnotatedWith(org.mule.sdk.api.annotation.Configurations.class))) {
      Optional<ComponentInformation> fieldComponent =
          extension.getFields().stream().map(SdkComponentsMinMuleVersionUtils::calculateFieldMinMuleVersion)
              .reduce(SdkComponentsMinMuleVersionUtils::max);
      if (fieldComponent.isPresent()) {
        extensionComponent.updateIfHigherMMV(fieldComponent.get(),
                format("Extension %s has min mule version %s because of its field %s.",
                        extension.getName(),
                        fieldComponent.get().getMinMuleVersion(), fieldComponent.get().getName()));
      }
      Optional<ComponentInformation> methodComponent =
          extension.getEnclosingMethods().map(SdkComponentsMinMuleVersionUtils::calculateMethodMinMuleVersion)
              .reduce(SdkComponentsMinMuleVersionUtils::max);
      if (methodComponent.isPresent()) {
        extensionComponent.updateIfHigherMMV(methodComponent.get(),
                format("Extension %s has min mule version %s because of its method %s.",
                        extension.getName(), methodComponent.get().getMinMuleVersion(),
                        methodComponent.get().getName()));
      }
    }
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(extension);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(extensionComponent.getMinMuleVersion())) {
        LOGGER
                .info("Calculated Min Mule Version is {} which is greater than the one set at the extension class level {}. Overriding it.",
                        extensionComponent.getMinMuleVersion(), classLevelMMV.get());
      } else {
        extensionComponent = new ComponentInformation(extension.getName(), classLevelMMV.get(),
                format("Extension %s has min mule version %s because it is the one set at the class level through the @MinMuleVersion annotation.",
                        extension.getName(), classLevelMMV.get()));
      }
    }
    return extensionComponent;
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
    ComponentInformation componentInformation = createConfigurationComponent(config, propagatedMinMuleVersion);
    LOGGER.info(componentInformation.getReason());
    return componentInformation.getMinMuleVersion();
  }

  private static ComponentInformation createConfigurationComponent(Type config, MuleVersion propagatedMinMuleVersion) {
    ComponentInformation configComponent = max(createComponentWithDefaultMMV("Configuration", config.getName()),
                                               new ComponentInformation(config.getName(), propagatedMinMuleVersion,
                                                                        format("Configuration %s has min mule version %s because it was propagated from the annotation (either @Configurations or @Config) used to reference this configuration.",
                                                                               config.getName(), propagatedMinMuleVersion)));
    Optional<Type> superType = config.getSuperType();
    if (superType.isPresent()) {
      ComponentInformation superConfigComponent = createConfigurationComponent(superType.get(), propagatedMinMuleVersion);
      configComponent.updateIfHigherMMV(superConfigComponent,
                                        format("Configuration %s has min mule version %s due to its super class %s.",
                                               config.getName(),
                                               superConfigComponent.getMinMuleVersion(), superConfigComponent.getName()));
    }
    Optional<ComponentInformation> annotationComponent =
        config.getAnnotations().map(SdkComponentsMinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (annotationComponent.isPresent()) {
      configComponent.updateIfHigherMMV(annotationComponent.get(),
                                        format("Configuration %s has min mule version %s because it is annotated with %s.",
                                               config.getName(),
                                               annotationComponent.get().getMinMuleVersion(),
                                               annotationComponent.get().getName()));
    }
    Optional<ComponentInformation> fieldComponent =
        config.getFields().stream().map(SdkComponentsMinMuleVersionUtils::calculateFieldMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (fieldComponent.isPresent()) {
      configComponent.updateIfHigherMMV(fieldComponent.get(),
                                        format("Configuration %s has min mule version %s because of its field %s.",
                                               config.getName(),
                                               fieldComponent.get().getMinMuleVersion(), fieldComponent.get().getName()));
    }
    Optional<ComponentInformation> methodComponent =
        config.getEnclosingMethods().map(SdkComponentsMinMuleVersionUtils::calculateMethodMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (methodComponent.isPresent()) {
      configComponent.updateIfHigherMMV(methodComponent.get(),
                                        format("Configuration %s has min mule version %s because of its method %s",
                                               config.getName(),
                                               methodComponent.get().getMinMuleVersion(), methodComponent.get().getName()));
    }
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(config);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(configComponent.getMinMuleVersion())) {
        LOGGER
                .info("Calculated Min Mule Version is {} which is greater than the one set at the configuration class level {}. Overriding it.",
                        configComponent.getMinMuleVersion(), classLevelMMV.get());
      } else {
        configComponent = new ComponentInformation(config.getName(), classLevelMMV.get(),
                format("Configuration %s has min mule version %s because it is the one set at the class level through the @MinMuleVersion annotation.",
                        config.getName(), classLevelMMV.get()));
      }
    }
    return configComponent;
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
    ComponentInformation componentInformation =
            max(createComponentWithDefaultMMV("Function", function.getName()),
                    new ComponentInformation(function.getName(), propagatedMinMuleVersion,
                            format("Function %s has min mule version %s because it was propagated from the @Functions annotation at the extension class used to add the function.",
                                    function.getName(), propagatedMinMuleVersion)));
    componentInformation = max(componentInformation, calculateMethodMinMuleVersion(function));
    LOGGER.info(componentInformation.getReason());
    return componentInformation.getMinMuleVersion();
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
    ComponentInformation operationComponent = createComponentWithDefaultMMV("Operation", operation.getName());
    operationComponent = max(operationComponent, new ComponentInformation(operation.getName(), propagatedMinMuleVersion,
            format("Operation %s has min mule version %s because it was propagated from the @Operations annotation at the extension class used to add the operation's container %s.",
                    operation.getName(), propagatedMinMuleVersion,
                    operationContainer.getName())));
    Optional<ComponentInformation> parameterComponent = operationContainer.getParameters().stream()
        .map(SdkComponentsMinMuleVersionUtils::calculateMethodParameterMinMuleVersion)
        .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (parameterComponent.isPresent()) {
      operationComponent.updateIfHigherMMV(parameterComponent.get(),
              format("Operation %s has min mule version %s because of its parameter %s.",
                      operation.getName(),
                      parameterComponent.get().getMinMuleVersion(),
                      parameterComponent.get().getName()));
    }
    operationComponent = max(operationComponent, calculateMethodMinMuleVersion(operation));
    LOGGER.info(operationComponent.getReason());
    return operationComponent.getMinMuleVersion();
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
    ComponentInformation connectionProviderComponent = createConnectionProviderComponent(connectionProvider);
    LOGGER.info(connectionProviderComponent.getReason());
    return connectionProviderComponent.getMinMuleVersion();
  }

  private static ComponentInformation createConnectionProviderComponent(Type connectionProvider) {
    ComponentInformation connectionProviderComponent =
            createComponentWithDefaultMMV("Connection Provider", connectionProvider.getName());
    Optional<Type> superType = connectionProvider.getSuperType();
    if (superType.isPresent()) {
      if (superType.get().isAssignableTo(ConnectionProvider.class)
              || superType.get().isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
        ComponentInformation superTypeComponent = createConnectionProviderComponent(superType.get());
        connectionProviderComponent.updateIfHigherMMV(superTypeComponent,
                format("Connection Provider %s has min mule version %s because of its super class %s.",
                        connectionProvider.getName(), superTypeComponent.getMinMuleVersion(),
                        superTypeComponent.getName()));
      }
    }
    Optional<ComponentInformation> interfaceComponent = connectionProvider.getImplementingInterfaces()
        .map(SdkComponentsMinMuleVersionUtils::calculateInterfaceMinMuleVersion).reduce(SdkComponentsMinMuleVersionUtils::max);
    interfaceComponent.ifPresent(comp -> connectionProviderComponent.updateIfHigherMMV(comp,
            format("Connection Provider %s has min mule version %s because of its interface %s.",
                    connectionProvider.getName(),
                    comp.getMinMuleVersion(),
                    comp.getName())));
    Optional<ComponentInformation> annotationComponent =
        connectionProvider.getAnnotations().map(SdkComponentsMinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    annotationComponent.ifPresent(comp -> connectionProviderComponent.updateIfHigherMMV(comp,
            format("Connection Provider %s has min mule version %s because it is annotated with %s.",
                    connectionProvider.getName(),
                    comp.getMinMuleVersion(),
                    comp.getName())));
    Optional<ComponentInformation> fieldComponent =
        connectionProvider.getFields().stream().map(SdkComponentsMinMuleVersionUtils::calculateFieldMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    fieldComponent.ifPresent(comp -> connectionProviderComponent.updateIfHigherMMV(comp,
            format("Connection Provider %s has min mule version %s because of its field %s.",
                    connectionProvider.getName(),
                    comp.getMinMuleVersion(),
                    comp.getName())));
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(connectionProvider);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo((connectionProviderComponent.getMinMuleVersion()))) {
        LOGGER
                .info("Calculated Min Mule Version is {} which is greater than the one set at the connection provider class level {}. Overriding it.",
                        connectionProviderComponent.getMinMuleVersion(), classLevelMMV.get());
      } else {
        return new ComponentInformation(connectionProvider.getName(), classLevelMMV.get(),
                format("Connection Provider %s has min mule version %s because it is the one set at the class level.",
                        connectionProvider.getName(), classLevelMMV.get()));
      }
    }
    return connectionProviderComponent;
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
    ComponentInformation componentInformation = createSourceComponent(source);
    LOGGER.info(componentInformation.getReason());
    return componentInformation.getMinMuleVersion();
  }

  private static ComponentInformation createSourceComponent(Type source) {
    ComponentInformation sourceComponent = createComponentWithDefaultMMV("Source", source.getName());
    Optional<Type> superType = source.getSuperType();
    if (superType.isPresent()) {
      ComponentInformation superTypeComponent;
      if (superType.get().isSameType(Source.class) || superType.get().isSameType(org.mule.sdk.api.runtime.source.Source.class)) {
        superTypeComponent = getEnforcedMinMuleVersion(superType.get());
      } else {
        superTypeComponent = createSourceComponent(superType.get());
      }
      sourceComponent.updateIfHigherMMV(superTypeComponent,
              format("Source %s has min mule version %s because of its super class %s.",
                      source.getName(),
                      superTypeComponent.getMinMuleVersion(), superTypeComponent.getName()));
    }
    if (source.isAssignableTo(Source.class)) {
      ComponentInformation genericsComponent = calculateSourceGenericsMinMuleVersion(source.getSuperTypeGenerics(Source.class));
      sourceComponent.updateIfHigherMMV(genericsComponent,
              format("Source %s has min mule version %s because it has a generic of type %s.",
                      source.getName(), genericsComponent.getMinMuleVersion(),
                      genericsComponent.getName()));
    }
    if (source.isAssignableTo(org.mule.sdk.api.runtime.source.Source.class)) {
      ComponentInformation genericsComponent =
              calculateSourceGenericsMinMuleVersion(source.getSuperTypeGenerics(org.mule.sdk.api.runtime.source.Source.class));
      sourceComponent.updateIfHigherMMV(genericsComponent,
              format("Source %s has min mule version %s because it has a generic of type %s.",
                      source.getName(), genericsComponent.getMinMuleVersion(),
                      genericsComponent.getName()));
    }
    Optional<ComponentInformation> interfaceComponent =
        source.getImplementingInterfaces().map(SdkComponentsMinMuleVersionUtils::calculateInterfaceMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (interfaceComponent.isPresent()) {
      sourceComponent.updateIfHigherMMV(interfaceComponent.get(),
              format("Source %s has min mule version %s because it implements interface %s.",
                      source.getName(), interfaceComponent.get().getMinMuleVersion(),
                      interfaceComponent.get().getName()));
    }
    Optional<ComponentInformation> annotationComponent =
        source.getAnnotations().map(SdkComponentsMinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (annotationComponent.isPresent()) {
      sourceComponent.updateIfHigherMMV(annotationComponent.get(),
              format("Source %s has min mule version %s because it is annotated with %s.",
                      source.getName(),
                      annotationComponent.get().getMinMuleVersion(),
                      annotationComponent.get().getName()));
    }
    Optional<ComponentInformation> fieldComponent =
        source.getFields().stream().map(SdkComponentsMinMuleVersionUtils::calculateFieldMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (fieldComponent.isPresent()) {
      sourceComponent.updateIfHigherMMV(fieldComponent.get(),
              format("Source %s has min mule version %s because of its field %s.", source.getName(),
                      fieldComponent.get().getMinMuleVersion(), fieldComponent.get().getName()));
    }
    Optional<ComponentInformation> methodComponent =
        source.getEnclosingMethods().map(SdkComponentsMinMuleVersionUtils::calculateMethodMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (methodComponent.isPresent()) {
      sourceComponent.updateIfHigherMMV(methodComponent.get(),
              format("Source %s has min mule version %s because of its method %s.", source.getName(),
                      methodComponent.get().getMinMuleVersion(), methodComponent.get().getName()));
    }
    Optional<MuleVersion> classLevelMMV = getMinMuleVersionFromAnnotations(source);
    if (classLevelMMV.isPresent()) {
      if (classLevelMMV.get().priorTo(sourceComponent.getMinMuleVersion())) {
        LOGGER
                .info("Calculated Min Mule Version is {} which is greater than the one set at the source class level {}. Overriding it.",
                        sourceComponent.getMinMuleVersion(), classLevelMMV);
      } else {
        sourceComponent = new ComponentInformation(source.getName(), classLevelMMV.get(),
                format("Source %s has min mule version %s because it is the one set at the class level.",
                        source.getName(), classLevelMMV.get()));
      }
    }
    return sourceComponent;
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

  private static ComponentInformation calculateMethodMinMuleVersion(MethodElement<?> method) {
    ComponentInformation methodComponent = createComponentWithDefaultMMV("Method", method.getName());
    Optional<ComponentInformation> annotationComponent =
        method.getAnnotations().map(SdkComponentsMinMuleVersionUtils::getEnforcedMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (annotationComponent.isPresent()) {
      methodComponent.updateIfHigherMMV(annotationComponent.get(),
              format("Method %s has min mule version %s because it is annotated with %s.",
                      method.getName(),
                      annotationComponent.get().getMinMuleVersion(),
                      annotationComponent.get().getName()));
    }
    Optional<ComponentInformation> parameterComponent =
        method.getParameters().stream().map(SdkComponentsMinMuleVersionUtils::calculateMethodParameterMinMuleVersion)
            .reduce(SdkComponentsMinMuleVersionUtils::max);
    if (parameterComponent.isPresent()) {
      methodComponent.updateIfHigherMMV(parameterComponent.get(),
              format("Method %s has min mule version %s because of its parameter %s.", method.getName(),
                      parameterComponent.get().getMinMuleVersion(),
                      parameterComponent.get().getName()));
    }
    ComponentInformation outputComponent = calculateOutputMinMuleVersion(method.getReturnType());
    methodComponent.updateIfHigherMMV(outputComponent,
            format("Method %s has min mule version %s because of its output type %s.", method.getName(),
                    outputComponent.getMinMuleVersion(), outputComponent.getName()));
    Optional<MuleVersion> operationLevelMMV = getMinMuleVersionFromAnnotations(method);
    if (operationLevelMMV.isPresent()) {
      if (operationLevelMMV.get().priorTo(methodComponent.getMinMuleVersion())) {
        LOGGER
                .info("Calculated Min Mule Version is {} which is greater than the one set at the method level {}. Overriding it.",
                        methodComponent.getMinMuleVersion(), operationLevelMMV.get());
      } else {
        methodComponent = new ComponentInformation(method.getName(), operationLevelMMV.get(),
                format("Method %s has min mule version %s because it is the one set at the method level through the @MinMuleVersion annotation.",
                        method.getName(), operationLevelMMV.get()));
      }
    }
    return methodComponent;
  }

  private static ComponentInformation calculateInterfaceMinMuleVersion(Type interfaceType) {
    ComponentInformation interfaceComponent = getEnforcedMinMuleVersion(interfaceType);
    if (belongsToSdkPackages(interfaceType.getTypeName())) {
      return interfaceComponent;
    }
    Optional<ComponentInformation> superInterface = interfaceType.getImplementingInterfaces()
        .map(SdkComponentsMinMuleVersionUtils::calculateInterfaceMinMuleVersion).reduce(SdkComponentsMinMuleVersionUtils::max);
    superInterface.ifPresent(componentInformation -> interfaceComponent.updateIfHigherMMV(componentInformation,
            format("Interface %s has min mule version %s because it implements %s.",
                    interfaceType.getName(),
                    componentInformation
                            .getMinMuleVersion(),
                    componentInformation
                            .getName())));
    return interfaceComponent;
  }

  private static ComponentInformation calculateSourceGenericsMinMuleVersion(List<Type> sourceGenerics) {
    Type outputType = sourceGenerics.get(0);
    Type attributesType = sourceGenerics.get(1);
    return max(getEnforcedMinMuleVersion(attributesType), calculateOutputMinMuleVersion(outputType));
  }

  private static ComponentInformation calculateOutputMinMuleVersion(Type outputType) {
    ComponentInformation componentInformation = createComponentWithDefaultMMV("Output type", outputType.getName());
    if (outputType.asMetadataType() instanceof ArrayType) {
      for (TypeGeneric typeGeneric : outputType.getGenerics()) {
        componentInformation = max(componentInformation, calculateOutputMinMuleVersion(typeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(Result.class) || outputType.isSameType(org.mule.sdk.api.runtime.operation.Result.class)) {
      componentInformation = max(componentInformation, getEnforcedMinMuleVersion(outputType));
      for (TypeGeneric resultTypeGeneric : outputType.getGenerics()) {
        componentInformation = max(componentInformation, getEnforcedMinMuleVersion(resultTypeGeneric.getConcreteType()));
      }
    } else if (outputType.isSameType(PagingProvider.class)
            || outputType.isSameType(org.mule.sdk.api.runtime.streaming.PagingProvider.class)) {
      componentInformation = max(componentInformation, getEnforcedMinMuleVersion(outputType));
      componentInformation =
              max(componentInformation, getEnforcedMinMuleVersion(outputType.getGenerics().get(1).getConcreteType()));
    } else {
      componentInformation = getEnforcedMinMuleVersion(outputType);
    }
    return componentInformation;
  }

  private static ComponentInformation calculateFieldMinMuleVersion(ExtensionParameter field) {
    ComponentInformation componentInformation = getMinMuleVersionFromAnnotations(field)
            .map(mmv -> new ComponentInformation(field.getName(), mmv,
                    format("Field %s has min mule version %s because it is annotated with @MinMuleVersion.",
                            field.getName(), mmv)))
            .orElse(createComponentWithDefaultMMV("Field", field.getName()));
    Type parameterType = field.getType();
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(parameterType.getTypeName())) {
      ComponentInformation typeComponent = getEnforcedMinMuleVersion(parameterType);
      componentInformation.updateIfHigherMMV(typeComponent,
              format("Field %s has min mule version %s because it is of type %s.", field.getName(),
                      typeComponent.getMinMuleVersion(), typeComponent.getName()));
    }
    for (Type annotation : field.getAnnotations().collect(toList())) {
      if (annotation.isSameType(Inject.class) && !parameterType.isSameType(Optional.class)) {
        // Parse injected classes but exclude Optionals (such as ForwardCompatibilityHelper)
        ComponentInformation typeComponent = getEnforcedMinMuleVersion(parameterType);
        componentInformation.updateIfHigherMMV(typeComponent,
                format("Field %s has min mule version %s because it is of type %s.",
                        field.getName(),
                        typeComponent.getMinMuleVersion(), typeComponent.getName()));
      }
      if (annotation.isSameType(Parameter.class)
              || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.Parameter.class)) {
        ComponentInformation parameterContainerComponent = calculateParameterContainerMinMuleVersion(parameterType);
        componentInformation.updateIfHigherMMV(parameterContainerComponent,
                format("Field %s has min mule version %s because it is a parameter container of type %s.",
                        field.getName(), parameterContainerComponent.getMinMuleVersion(),
                        parameterContainerComponent.getName()));
      }
      if (annotation.isSameType(ParameterGroup.class)
              || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.ParameterGroup.class)) {
        ComponentInformation parameterGroupComponent = calculateParameterContainerMinMuleVersion(parameterType);
        componentInformation.updateIfHigherMMV(parameterGroupComponent,
                format("Field %s has min mule version %s because it is a parameter group of type %s.",
                        field.getName(), parameterGroupComponent.getMinMuleVersion(),
                        parameterGroupComponent.getName()));
      }
      if (annotation.isSameType(Connection.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Connection.class)) {
        // Sources inject the ConnectionProvider instead of the connection
        if (parameterType.isAssignableTo(ConnectionProvider.class)
                || parameterType.isAssignableTo(org.mule.sdk.api.connectivity.ConnectionProvider.class)) {
          ComponentInformation connectionProviderComponent = createConnectionProviderComponent(parameterType);
          componentInformation.updateIfHigherMMV(connectionProviderComponent,
                  format("Field %s has min mule version %s because it is a connection provider of type %s.",
                          field.getName(), connectionProviderComponent.getMinMuleVersion(),
                          connectionProviderComponent.getName()));
        }
      }
      if (annotation.isSameType(Config.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Config.class)) {
        ComponentInformation configurationComponent =
                createConfigurationComponent(parameterType, getEnforcedMinMuleVersion(annotation).getMinMuleVersion());
        componentInformation.updateIfHigherMMV(configurationComponent,
                format("Field %s has min mule version %s because it is a configuration of type %s.",
                        field.getName(), configurationComponent.getMinMuleVersion(),
                        configurationComponent.getName()));
      }
      ComponentInformation annotationComponent = getEnforcedMinMuleVersion(annotation);
      componentInformation.updateIfHigherMMV(annotationComponent,
              format("Field %s has min mule version %s because it is annotated with %s.",
                      field.getName(),
                      annotationComponent.getMinMuleVersion(), annotationComponent.getName()));
    }
    return componentInformation;
  }

  private static ComponentInformation calculateMethodParameterMinMuleVersion(ExtensionParameter methodParameter) {
    ComponentInformation methodParameterComponent = createComponentWithDefaultMMV("Parameter", methodParameter.getName());
    for (Type annotation : methodParameter.getAnnotations().collect(toList())) {
      if (annotation.isSameType(ParameterGroup.class)
              || annotation.isSameType(org.mule.runtime.extension.api.annotation.param.ParameterGroup.class)) {
        methodParameterComponent =
                max(methodParameterComponent, calculateParameterContainerMinMuleVersion(methodParameter.getType()));
      }
      if (annotation.isSameType(Config.class) || annotation.isSameType(org.mule.sdk.api.annotation.param.Config.class)) {
        methodParameterComponent =
                max(methodParameterComponent,
                        createConfigurationComponent(methodParameter.getType(),
                                getEnforcedMinMuleVersion(annotation).getMinMuleVersion()));
      }
      ComponentInformation annotationComponent = getEnforcedMinMuleVersion(annotation);
      methodParameterComponent.updateIfHigherMMV(annotationComponent,
              format("Parameter %s has min mule version %s because it is annotated with %s.",
                      methodParameter.getName(), annotationComponent.getMinMuleVersion(),
                      annotationComponent.getName()));
    }
    // Parse sdk fields (this also catches automatically injected fields)
    if (belongsToSdkPackages(methodParameter.getType().getTypeName())) {
      ComponentInformation typeComponent = getEnforcedMinMuleVersion(methodParameter.getType());
      methodParameterComponent.updateIfHigherMMV(typeComponent,
              format("Parameter %s has min mule version %s because it is of type %s.",
                      methodParameter.getName(), typeComponent.getMinMuleVersion(),
                      typeComponent.getName()));
    }
    return methodParameterComponent;
  }

  private static ComponentInformation calculateParameterContainerMinMuleVersion(Type containerType) {
    Optional<MuleVersion> minMuleVersionAnnotation = getMinMuleVersionFromAnnotations(containerType);
    if (minMuleVersionAnnotation.isPresent()) {
      String reason = format("Parameter container %s has min mule version %s because it is annotated with @MinMuleVersion",
              containerType.getName(), minMuleVersionAnnotation.get());
      return new ComponentInformation(containerType.getName(), minMuleVersionAnnotation.get(), reason);
    }
    ComponentInformation componentInformation = createComponentWithDefaultMMV("Parameter container", containerType.getName());
    componentInformation = containerType.getAnnotations().map(SdkComponentsMinMuleVersionUtils::getEnforcedMinMuleVersion)
        .reduce(componentInformation, SdkComponentsMinMuleVersionUtils::max);
    for (FieldElement field : containerType.getFields()) {
      componentInformation = max(componentInformation, calculateFieldMinMuleVersion(field));
    }
    return componentInformation;
  }

  private static ComponentInformation getEnforcedMinMuleVersion(Type type) {
    if (type.isAnnotatedWith(DoNotEnforceMinMuleVersion.class)) {
      return new ComponentInformation(type.getTypeName(), FIRST_MULE_VERSION,
              format("%s has the default base min mule version %s because it is annotated with @DoNotEnforceMinMuleVersion.",
                      type.getTypeName(), FIRST_MULE_VERSION));
    }
    Optional<MuleVersion> mmv = getMinMuleVersionFromAnnotations(type);
    return mmv
            .map(muleVersion -> new ComponentInformation(type.getTypeName(), muleVersion,
                    format("%s has min mule version %s because it is annotated with @MinMuleVersion.",
                            type.getTypeName(), muleVersion)))
            .orElseGet(() -> createComponentWithDefaultMMV("Type", type.getTypeName()));
  }

  private static Optional<MuleVersion> getMinMuleVersionFromAnnotations(WithAnnotations type) {
    return type.getValueFromAnnotation(MinMuleVersion.class)
            .map(fetcher -> new MuleVersion(fetcher.getStringValue(MinMuleVersion::value)));
  }

  private static ComponentInformation max(ComponentInformation currentMax, ComponentInformation candidate) {
    if (currentMax.getMinMuleVersion().atLeast(candidate.getMinMuleVersion())) {
      return currentMax;
    }
    return candidate;
  }

  private static ComponentInformation createComponentWithDefaultMMV(String componentDescription, String componentName) {
    return new ComponentInformation(componentName, FIRST_MULE_VERSION,
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
}