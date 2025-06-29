/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
import static org.mule.runtime.module.extension.internal.lifecycle.ExtensionOnMuleContextDisposedNotificationListener.registerLifecycleListenerForOnContextDisposed;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterClasses;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSubtypeClasses;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.el.DefaultBindingContextBuilder;
import org.mule.runtime.core.internal.el.DefaultExpressionModuleBuilder;
import org.mule.runtime.core.internal.transformer.simple.StringToEnum;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;
import org.mule.runtime.extension.api.runtime.function.FunctionExecutor;
import org.mule.runtime.extension.api.runtime.function.FunctionParameterDefaultValueResolverFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.FunctionExecutorModelProperty;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Activator implementation for registered {@link ExtensionModel extensions}
 *
 * @since 4.0
 */
public final class ExtensionActivator implements Startable, Stoppable {

  private final MuleContext muleContext;
  private final Set<Class<? extends Enum>> enumTypes = new HashSet<>();
  private final List<Object> lifecycleAwareElements = new LinkedList<>();

  ExtensionActivator(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  void activateExtension(ExtensionModel extensionModel) {
    registerEnumTransformers(extensionModel);
    registerAsModuleDefinition(extensionModel);
    registerLifecycleListenerForOnContextDisposed(muleContext.getNotificationManager(),
                                                  muleContext.getExecutionClassLoader(),
                                                  extensionModel);
  }

  private void registerEnumTransformers(ExtensionModel extensionModel) {
    ClassLoader classLoader = getClassLoader(extensionModel);
    Set<Class<?>> parameterClasses = new HashSet<>();

    parameterClasses.addAll(getParameterClasses(extensionModel, classLoader));
    parameterClasses.addAll(getSubtypeClasses(extensionModel, classLoader));

    parameterClasses.stream()
        .filter(type -> Enum.class.isAssignableFrom(type))
        .forEach(type -> {
          final Class<Enum> enumClass = (Class<Enum>) type;
          if (enumTypes.add(enumClass)) {
            try {
              StringToEnum stringToEnum = new StringToEnum(enumClass);
              registerObject(muleContext, stringToEnum.getName(), stringToEnum);
            } catch (MuleException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not register transformer for enum "
                  + enumClass.getName()), e);
            }
          }
        });
  }

  private void registerAsModuleDefinition(ExtensionModel extensionModel) {
    if (extensionModel.getFunctionModels().isEmpty() && extensionModel.getTypes().isEmpty()) {
      return;
    }

    ExpressionModule expressionModule = extensionAsModuleDefinition(extensionModel);
    try {
      final BindingContext bindingContext = new DefaultBindingContextBuilder().addModule(expressionModule).build();
      registerObject(muleContext, extensionModel.getName() + "GlobalBindingContextProvider",
                     (GlobalBindingContextProvider) () -> bindingContext);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(e.getMessage()), e);
    }
  }

  private ExpressionModule extensionAsModuleDefinition(ExtensionModel extensionModel) {
    ModuleNamespace namespace = new ModuleNamespace(extensionModel.getXmlDslModel().getPrefix());
    ExpressionModule.Builder moduleBuilder = new DefaultExpressionModuleBuilder(namespace);

    if (!extensionModel.getFunctionModels().isEmpty()) {
      addExtensionFunctions(extensionModel.getFunctionModels().stream(), moduleBuilder);
    }
    if (!extensionModel.getTypes().isEmpty()) {
      addExtensionTypes(extensionModel.getTypes().stream(), moduleBuilder);
    }

    return moduleBuilder.build();
  }

  private void addExtensionTypes(Stream<ObjectType> objectTypes, ExpressionModule.Builder module) {
    objectTypes.forEach(module::addType);
  }

  private void addExtensionFunctions(Stream<FunctionModel> functions, ExpressionModule.Builder module) {
    final FunctionParameterDefaultValueResolverFactory valueResolverFactory = (defaultValue, type) -> context -> {
      ExtendedExpressionManager em = muleContext.getExpressionManager();
      String value = String.valueOf(defaultValue);
      return em.isExpression(value) ? em.evaluate(value, type, context) : new TypedValue<>(defaultValue, type);
    };

    functions.forEach(function -> function.getModelProperty(FunctionExecutorModelProperty.class).ifPresent(mp -> {
      FunctionExecutor executor = mp.getExecutorFactory().createExecutor(function, valueResolverFactory);
      lifecycleAwareElements.add(executor);
      module.addBinding(function.getName(), new TypedValue<>(executor, fromFunction(executor)));
    }));
  }

  @Override
  public void start() throws MuleException {
    for (Object element : lifecycleAwareElements) {
      initialiseIfNeeded(element, muleContext);
      startIfNeeded(element);
    }
  }

  @Override
  public void stop() throws MuleException {
    for (Object element : lifecycleAwareElements) {
      stopIfNeeded(element);
    }
    lifecycleAwareElements.clear();
    enumTypes.clear();
  }

  public Set<Class<? extends Enum>> getEnumTypes() {
    return enumTypes;
  }
}
