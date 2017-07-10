/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.FunctionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasFunctionDeclarer;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.property.FunctionExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.WithFunctionContainers;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.function.ReflectiveFunctionExecutorFactory;

import java.lang.reflect.Method;
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

  void declareFunctions(ExtensionDeclarer extensionDeclarer, HasFunctionDeclarer declarer,
                        WithFunctionContainers functionContainers) {
    functionContainers.getFunctionContainers()
        .forEach(functionContainer -> declareFunctions(extensionDeclarer, declarer, functionContainer));
  }

  void declareFunctions(ExtensionDeclarer extensionDeclarer, HasFunctionDeclarer declarer,
                        FunctionContainerElement functionContainerElement) {
    declareFunctions(extensionDeclarer, declarer, functionContainerElement.getDeclaringClass(),
                     functionContainerElement.getFunctions());
  }

  void declareFunctions(ExtensionDeclarer extensionDeclarer,
                        HasFunctionDeclarer declarer,
                        final Class<?> methodOwnerClass,
                        List<MethodElement> functions) {

    for (MethodElement methodElement : functions) {
      Class<?> declaringClass = methodOwnerClass != null ? methodOwnerClass : methodElement.getDeclaringClass();
      checkIsNotAnExtension(declaringClass);

      final Method method = methodElement.getMethod();
      final Optional<ExtensionParameter> configParameter = loader.getConfigParameter(methodElement);

      if (configParameter.isPresent()) {
        throw new IllegalModelDefinitionException(format("Function '%s' requires a config parameter, but that is not allowed. "
            + "Remove such parameter.",
                                                         method.getName()));
      }

      HasFunctionDeclarer actualDeclarer =
          (HasFunctionDeclarer) loader.selectDeclarerBasedOnConfig(extensionDeclarer, (Declarer) declarer, configParameter,
                                                                   empty());

      if (functionDeclarers.containsKey(methodElement)) {
        actualDeclarer.withFunction(functionDeclarers.get(methodElement));
        continue;
      }

      final FunctionDeclarer function = actualDeclarer.withFunction(methodElement.getAlias())
          .withModelProperty(new ImplementingMethodModelProperty(method))
          .withModelProperty(new FunctionExecutorModelProperty(new ReflectiveFunctionExecutorFactory<>(declaringClass,
                                                                                                       method)));

      function.withOutput().ofType(getMethodReturnType(method, loader.getTypeLoader()));

      ParameterDeclarationContext declarationContext = new ParameterDeclarationContext(FUNCTION, function.getDeclaration());
      loader.getMethodParametersLoader().declare(function, methodElement.getParameters(), declarationContext);
      functionDeclarers.put(methodElement, function);
    }
  }

  private void checkIsNotAnExtension(Class<?> type) {
    if (type.isAssignableFrom(getExtensionType()) || getExtensionType().isAssignableFrom(type)) {
      throw new IllegalOperationModelDefinitionException(
                                                         format("Function class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                type.getName(), getExtensionType().getName()));
    }
  }

}
