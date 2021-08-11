/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.getExceptionEnricherFactory;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConfigParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConnectionParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getSourceParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forSource;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SdkSourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSdkSourceFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class JavaSourceModelParser extends AbstractJavaExecutableComponentModelParser implements SourceModelParser {

  private final SourceElement sourceElement;

  private final Class<?> sourceClass;
  private final Optional<ExtensionParameter> configParameter;
  private final Optional<ExtensionParameter> connectionParameter;


  public JavaSourceModelParser(ExtensionElement extensionElement,
                               SourceElement sourceElement,
                               ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);
    this.sourceElement = sourceElement;

    sourceClass = sourceElement.getDeclaringClass().get();

    configParameter = getConfigParameter(sourceElement);
    connectionParameter = getConnectionParameter(sourceElement);

    parseStructure();
    collectAdditionalModelProperties();
  }

  @Override
  public String getName() {
    return sourceElement.getAlias();
  }

  @Override
  public String getDescription() {
    return sourceElement.getDescription();
  }

  @Override
  public boolean emitsResponse() {
    return sourceElement.isAnnotatedWith(EmitsResponse.class);
  }

  @Override
  public SdkSourceFactoryModelProperty getSourceFactoryModelProperty() {
    return new SdkSourceFactoryModelProperty(new DefaultSdkSourceFactory(sourceClass));
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    return getSourceParameterGroupParsers(sourceElement.getParameters(), forSource(getName()));
  }

  @Override
  public Optional<SourceCallbackModelParser> getOnSuccessCallbackParser() {
    return parseSourceCallback(sourceElement.getOnResponseMethod());
  }

  @Override
  public Optional<SourceCallbackModelParser> getOnErrorCallbackParser() {
    return parseSourceCallback(sourceElement.getOnErrorMethod());
  }

  @Override
  public Optional<SourceCallbackModelParser> getOnTerminateCallbackParser() {
    return parseSourceCallback(sourceElement.getOnTerminateMethod());
  }

  @Override
  public Optional<SourceCallbackModelParser> getOnBackPressureCallbackParser() {
    return parseSourceCallback(sourceElement.getOnBackPressureMethod());
  }

  @Override
  public boolean runsOnPrimaryNodeOnly() {
    // TODO: This should partially replace ClusterSupportEnricher
    return false;
  }

  @Override
  public boolean isIgnored() {
    return IntrospectionUtils.isIgnored(sourceElement, loadingContext);
  }

  @Override
  public Optional<MediaTypeModelProperty> getMediaTypeModelProperty() {
    return sourceElement.getAnnotation(MediaType.class)
        .map(a -> new MediaTypeModelProperty(a.value(), a.strict()));
  }

  @Override
  public Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty() {
    return getExceptionEnricherFactory(sourceElement).map(ExceptionHandlerModelProperty::new);
  }

  @Override
  public boolean hasConfig() {
    return configParameter.isPresent();
  }

  private void collectAdditionalModelProperties() {
    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(sourceElement));
    additionalModelProperties.add(new ImplementingTypeModelProperty(sourceClass));
    additionalModelProperties.add(new SourceCallbackModelProperty(
                                                                  extractJavaMethod(sourceElement.getOnResponseMethod()),
                                                                  extractJavaMethod(sourceElement.getOnErrorMethod()),
                                                                  extractJavaMethod(sourceElement.getOnTerminateMethod()),
                                                                  extractJavaMethod(sourceElement.getOnBackPressureMethod())));
  }

  private Optional<Method> extractJavaMethod(Optional<MethodElement> method) {
    return method.flatMap(MethodElement::getMethod);
  }

  @Override
  protected String getComponentTypeName() {
    return "Source";
  }

  private void parseStructure() {
    // TODO: MULE-9220 - Add a syntax validator which checks that the parser doesn't implement
    validateLifecycle(sourceElement, Startable.class);
    validateLifecycle(sourceElement, Stoppable.class);

    List<Type> sourceGenerics = sourceElement.getSuperClassGenerics();
    if (sourceGenerics.size() != 2) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalModelDefinitionException(format("Message source class '%s' was expected to have 2 generic types "
          + "(one for the Payload type and another for the Attributes type) but %d were found",
                                                       getName(), sourceGenerics.size()));
    }

    resolveOutputTypes();
    parseComponentByteStreaming(sourceElement);

    connected = connectionParameter.isPresent();
    parseComponentConnectivity(sourceElement);
  }

  private void resolveOutputTypes() {
    final MetadataType returnMetadataType = sourceElement.getReturnMetadataType();

    // TODO: Should be possible to parse dynamic types right here
    outputType = new DefaultOutputModelParser(returnMetadataType, false);
    outputAttributesType = new DefaultOutputModelParser(sourceElement.getAttributesMetadataType(), false);
  }

  private void validateLifecycle(SourceElement sourceType, Class<?> lifecycleType) {
    if (sourceType.isAssignableTo(lifecycleType)) {
      throw new IllegalSourceModelDefinitionException(format(
                                                             "Source class '%s' implements lifecycle interface '%s'. Sources are only not allowed to implement '%s' and '%s'",
                                                             sourceType.getName(), lifecycleType,
                                                             Initialisable.class.getSimpleName(),
                                                             Disposable.class.getSimpleName()));
    }
  }

  private Optional<SourceCallbackModelParser> parseSourceCallback(Optional<MethodElement> methodElement) {
    return methodElement
        .map(method -> new JavaSourceCallbackModelParser(getParameterGroupParsers(
                                                                                  method.getParameters(),
                                                                                  forSource(getName()))));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof JavaSourceModelParser) {
      return sourceElement.equals(((JavaSourceModelParser) o).sourceElement);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(sourceElement);
  }

  private static class JavaSourceCallbackModelParser implements SourceCallbackModelParser {

    private final List<ParameterGroupModelParser> groupModelParsers;

    public JavaSourceCallbackModelParser(List<ParameterGroupModelParser> groupModelParsers) {
      this.groupModelParsers = groupModelParsers;
    }

    @Override
    public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
      return groupModelParsers;
    }
  }
}
