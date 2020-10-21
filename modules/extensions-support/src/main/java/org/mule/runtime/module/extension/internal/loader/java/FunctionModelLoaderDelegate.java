/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isIgnored;

import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.FunctionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasFunctionDeclarer;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.WithFunctionContainers;
import org.mule.runtime.module.extension.internal.loader.java.property.FunctionExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.function.ReflectiveFunctionExecutorFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for declaring functions through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class FunctionModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String FUNCTION = "Function";

  private final Map<MethodElement, FunctionDeclarer> functionDeclarers = new HashMap<>();

  FunctionModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareFunctions(ExtensionDeclarer extensionDeclarer,
                        HasFunctionDeclarer declarer,
                        WithFunctionContainers functionContainers,
                        ExtensionLoadingContext loadingContext) {
    functionContainers.getFunctionContainers()
        .forEach(functionContainer -> declareFunctions(extensionDeclarer, declarer, functionContainer, loadingContext));
  }

  void declareFunctions(ExtensionDeclarer extensionDeclarer,
                        HasFunctionDeclarer declarer,
                        FunctionContainerElement functionContainerElement,
                        ExtensionLoadingContext loaderContext) {
    declareFunctions(extensionDeclarer, declarer, functionContainerElement,
                     functionContainerElement.getFunctions(), loaderContext);
  }

  void declareFunctions(ExtensionDeclarer extensionDeclarer,
                        HasFunctionDeclarer declarer,
                        FunctionContainerElement methodOwnerClass,
                        List<FunctionElement> functions,
                        ExtensionLoadingContext loaderContext) {

    for (FunctionElement function : functions) {

      if (isIgnored(function, loaderContext)) {
        continue;
      }

      FunctionContainerElement functionOwner = methodOwnerClass != null ? methodOwnerClass : function.getEnclosingType();

      checkIsNotAnExtension(functionOwner);

      final Optional<ExtensionParameter> configParameter = loader.getConfigParameter(function);

      if (configParameter.isPresent()) {
        throw new IllegalModelDefinitionException(format("Function '%s' requires a config parameter, but that is not allowed. "
            + "Remove such parameter.",
                                                         function.getName()));
      }

      HasFunctionDeclarer actualDeclarer =
          (HasFunctionDeclarer) loader.selectDeclarerBasedOnConfig(extensionDeclarer, (Declarer) declarer, configParameter,
                                                                   empty());

      if (functionDeclarers.containsKey(function)) {
        actualDeclarer.withFunction(functionDeclarers.get(function));
        continue;
      }

      final FunctionDeclarer functionDeclarer = actualDeclarer.withFunction(function.getAlias());
      function.getMethod().ifPresent(method -> {
        functionDeclarer
            .withModelProperty(new ImplementingMethodModelProperty(method));
        function.getDeclaringClass().ifPresent(clazz -> functionDeclarer
            .withModelProperty(new FunctionExecutorModelProperty(new ReflectiveFunctionExecutorFactory<>(clazz, method))));
      });

      functionDeclarer.withOutput().ofType(function.getReturnMetadataType());

      ParameterDeclarationContext declarationContext =
          new ParameterDeclarationContext(FUNCTION, functionDeclarer.getDeclaration());
      loader.getMethodParametersLoader().declare(functionDeclarer, function.getParameters(), declarationContext);
      functionDeclarers.put(function, functionDeclarer);
    }
  }

  private void checkIsNotAnExtension(FunctionContainerElement type) {
    if (type.isAssignableFrom(getExtensionElement()) || getExtensionElement().isAssignableFrom(type)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Function class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                type.getName(), getExtensionElement().getName()));
    }
  }

}
