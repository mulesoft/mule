/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.hash;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConfigParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConnectionParameter;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils;
import org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class JavaOperationModelParser implements OperationModelParser {

  private final ExtensionElement extensionElement;
  private final OperationElement operationMethod;
  private final OperationContainerElement methodOwnerClass;
  private final OperationContainerElement methodOwner;
  private final OperationContainerElement enclosingType;
  private final ExtensionLoadingContext loadingContext;
  private final boolean supportsConfig;
  private final LazyValue<Optional<ExtensionParameter>> configParameter;
  private final LazyValue<Optional<ExtensionParameter>> connectionParameter;
  private final List<ModelProperty> additionalModelProperties = new LinkedList<>();

  public JavaOperationModelParser(ExtensionElement extensionElement,
                                  OperationContainerElement methodOwnerClass,
                                  OperationElement operationMethod,
                                  ExtensionLoadingContext loadingContext,
                                  boolean supportsConfig) {
    this.extensionElement = extensionElement;
    this.operationMethod = operationMethod;
    this.methodOwnerClass = methodOwnerClass;
    this.loadingContext = loadingContext;
    this.supportsConfig = supportsConfig;

    methodOwner = operationMethod.getEnclosingType();
    enclosingType = methodOwnerClass != null ? methodOwnerClass : methodOwner;
    checkOperationIsNotAnExtension();

    configParameter = new LazyValue<>(() -> getConfigParameter(operationMethod));
    connectionParameter = new LazyValue<>(() -> getConnectionParameter(operationMethod));

    if (!isNonBlocking()) {
      throw new IllegalOperationModelDefinitionException(format("Scope '%s' does not declare a '%s' parameter. One is required " +
              "for all operations that receive and execute a Chain of other components",
          getName(),
          CompletionCallback.class.getSimpleName()));
    }
    collectAdditionalModelProperties();
  }

  @Override
  public String getName() {
    return operationMethod.getAlias();
  }

  @Override
  public String getDescription() {
    return operationMethod.getDescription();
  }

  @Override
  public boolean isIgnored() {
    return IntrospectionUtils.isIgnored(operationMethod, loadingContext);
  }

  @Override
  public boolean isScope() {
    return operationMethod.getParameters().stream().anyMatch(ModelLoaderUtils::isProcessorChain);
  }

  @Override
  public boolean isConnected() {
    return connectionParameter.get().isPresent();
  }

  @Override
  public boolean isNonBlocking() {
    return ModelLoaderUtils.isNonBlocking(operationMethod);
  }

  @Override
  public Optional<MediaTypeModelProperty> getMediaTypeModelProperty() {
    return operationMethod.getAnnotation(MediaType.class)
        .map(a -> new MediaTypeModelProperty(a.value(), a.strict()));
  }

  @Override
  public boolean hasConfig() {
    return configParameter.get().isPresent();
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return unmodifiableList(additionalModelProperties);
  }

  private void checkOperationIsNotAnExtension() {
    if (methodOwner.isAssignableFrom(extensionElement) || extensionElement.isAssignableFrom(methodOwner)) {
      throw new IllegalOperationModelDefinitionException(
          format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
              methodOwner.getName(), extensionElement.getName()));
    }
  }

  private void collectAdditionalModelProperties() {
    additionalModelProperties.add(new ExtensionOperationDescriptorModelProperty(operationMethod));

    Optional<Method> method = operationMethod.getMethod();
    Optional<Class<?>> declaringClass = enclosingType.getDeclaringClass();

    if (method.isPresent() && declaringClass.isPresent()) {
      additionalModelProperties.add(new ImplementingMethodModelProperty(method.get()));
      additionalModelProperties.add(new CompletableComponentExecutorModelProperty(
          new CompletableOperationExecutorFactory(declaringClass.get(), method.get())));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof JavaOperationModelParser) {
      return operationMethod.equals(((JavaOperationModelParser) o).operationMethod);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(operationMethod);
  }
}
