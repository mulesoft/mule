/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getConfigParameter;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forFunction;

import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.loader.java.property.FunctionExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.runtime.function.ReflectiveFunctionExecutorFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * {@link FunctionModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaFunctionModelParser extends AbstractJavaExecutableComponentModelParser implements FunctionModelParser {

  private final FunctionElement functionElement;

  public JavaFunctionModelParser(ExtensionElement extensionElement,
                                 FunctionElement functionElement,
                                 ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);

    this.functionElement = functionElement;

    parseStructure();
    collectAdditionalModelProperties();
  }

  @Override
  public String getName() {
    return functionElement.getAlias();
  }

  @Override
  public String getDescription() {
    return functionElement.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    return getParameterGroupParsers(functionElement.getParameters(), forFunction(getName()));
  }

  @Override
  public boolean isIgnored() {
    return IntrospectionUtils.isIgnored(functionElement, loadingContext);
  }

  @Override
  public FunctionExecutorModelProperty getFunctionExecutorModelProperty() {
    return new FunctionExecutorModelProperty(new ReflectiveFunctionExecutorFactory<>(
                                                                                     functionElement.getDeclaringClass().get(),
                                                                                     functionElement.getMethod().get()));
  }

  @Override
  protected String getComponentTypeName() {
    return "Function";
  }

  private void collectAdditionalModelProperties() {
    additionalModelProperties.add(new ImplementingMethodModelProperty(functionElement.getMethod().get()));
  }

  private void parseStructure() {
    checkIsNotAnExtension();

    final Optional<ExtensionParameter> configParameter = getConfigParameter(functionElement);
    if (configParameter.isPresent()) {
      throw new IllegalModelDefinitionException(format("Function '%s' requires a config parameter, but that is not allowed. "
          + "Remove such parameter.", getName()));
    }

    outputType = new DefaultOutputModelParser(functionElement.getReturnMetadataType(), false);
  }

  private void checkIsNotAnExtension() {
    final FunctionContainerElement type = functionElement.getEnclosingType();
    if (type.isAssignableFrom(extensionElement) || extensionElement.isAssignableFrom(type)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Function class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                type.getName(), extensionElement.getName()));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof JavaFunctionModelParser) {
      return functionElement.equals(((JavaFunctionModelParser) o).functionElement);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(functionElement);
  }
}
