/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.java.OperationModelLoaderDelegate.checkDefinition;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclarer;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveOperationExecutorFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for declaring routers through a {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
final class RouterModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String CONSTRUCT = "Construct";
  private static final List<Class<?>> VALID_CALLBACK_PARAMETERS = asList(RouterCompletionCallback.class,
                                                                         VoidCompletionCallback.class);

  private final Map<MethodElement, ConstructDeclarer> constructDeclarers = new HashMap<>();


  RouterModelLoaderDelegate(DefaultJavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareRouter(ExtensionDeclarer extensionDeclarer,
                     HasOperationDeclarer ownerDeclarer,
                     OperationContainerElement enclosingType,
                     OperationElement routerMethod,
                     Optional<ExtensionParameter> configParameter,
                     Optional<ExtensionParameter> connectionParameter) {

    checkDefinition(!configParameter.isPresent(),
                    format("Scope '%s' requires a config, but that is not allowed, remove such parameter",
                           routerMethod.getName()));

    checkDefinition(!connectionParameter.isPresent(),
                    format("Scope '%s' requires a connection, but that is not allowed, remove such parameter",
                           routerMethod.getName()));

    HasConstructDeclarer actualDeclarer =
        (HasConstructDeclarer) loader.selectDeclarerBasedOnConfig(extensionDeclarer, (Declarer) ownerDeclarer,
                                                                  configParameter, connectionParameter);

    if (constructDeclarers.containsKey(routerMethod)) {
      actualDeclarer.withConstruct(constructDeclarers.get(routerMethod));
      return;
    }

    final ConstructDeclarer router = actualDeclarer.withConstruct(routerMethod.getAlias());
    router.withModelProperty(new ExtensionOperationDescriptorModelProperty(routerMethod));
    Optional<Method> method = routerMethod.getMethod();
    Optional<Class<?>> declaringClass = enclosingType.getDeclaringClass();

    if (method.isPresent() && declaringClass.isPresent()) {
      router
          .withModelProperty(new ImplementingMethodModelProperty(method.get()))
          .withModelProperty(new CompletableComponentExecutorModelProperty(new ReflectiveOperationExecutorFactory(declaringClass
              .get(),
                                                                                                                  method.get())));
    }


    processMimeType(router, routerMethod);

    List<ExtensionParameter> callbackParameters = routerMethod.getParameters().stream()
        .filter(p -> VALID_CALLBACK_PARAMETERS.stream().anyMatch(validType -> p.getType().isSameType(validType)))
        .collect(toList());

    List<ExtensionParameter> routes = routerMethod.getParameters().stream().filter(this::isRoute).collect(toList());

    checkDefinition(!callbackParameters.isEmpty(),
                    format("Router '%s' does not declare a parameter with one of the types '%s'. One is required.",
                           routerMethod.getAlias(), VALID_CALLBACK_PARAMETERS));

    checkDefinition(!routes.isEmpty(),
                    format("Router '%s' does not declare a '%s' parameter. One is required.",
                           routerMethod.getAlias(), Route.class.getSimpleName()));

    checkDefinition(callbackParameters.size() <= 1,
                    format("Router '%s' defines more than one CompletionCallback parameters. Only one is allowed",
                           routerMethod.getAlias()));

    checkDefinition(isVoid(routerMethod), format("Router '%s' is not declared in a void method.", routerMethod.getAlias()));

    List<ExtensionParameter> nonRouteParameters = routerMethod.getParameters().stream()
        .filter(p -> !isRoute(p) && !callbackParameters.contains(p))
        .collect(toList());

    declareParameters(router, nonRouteParameters, routerMethod.getEnclosingType().getParameters(),
                      new ParameterDeclarationContext(CONSTRUCT, router.getDeclaration()));

    declareRoutes(router, routes);
  }

  private void declareRoutes(ConstructDeclarer router, List<ExtensionParameter> routes) {
    routes.forEach(route -> {
      NestedRouteDeclarer routeDeclarer = router
          .withRoute(route.getAlias())
          .describedAs(route.getDescription())
          .withMinOccurs(route.isRequired() ? 1 : 0);

      route.getType().getDeclaringClass()
          .ifPresent(clazz -> routeDeclarer.withModelProperty(new ImplementingTypeModelProperty(clazz)));

      final List<FieldElement> parameters = route.getType().getAnnotatedFields(Parameter.class);
      loader.getFieldParametersLoader().declare(routeDeclarer, parameters,
                                                new ParameterDeclarationContext(CONSTRUCT, router.getDeclaration()));
    });
  }

  private boolean isRoute(ExtensionParameter parameter) {
    return parameter.getType().isAssignableTo(Route.class);
  }

}
