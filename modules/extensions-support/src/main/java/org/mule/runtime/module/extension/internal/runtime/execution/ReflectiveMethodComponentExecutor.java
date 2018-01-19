/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.operation.ReflectiveMethodOperationExecutor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;

/**
 * Executes a task associated to a {@link ExecutionContext} by invoking a given {@link Method}
 *
 * @param <M> the generic type of the associated {@link ComponentModel}
 * @since 4.0
 */
public class ReflectiveMethodComponentExecutor<M extends ComponentModel>
    implements MuleContextAware, Lifecycle, OperationArgumentResolverFactory<M> {

  private static class NoArgumentsResolverDelegate implements ArgumentResolverDelegate {

    private static final LazyValue[] EMPTY = new LazyValue[] {};

    @Override
    public LazyValue<Object>[] resolve(ExecutionContext executionContext, Class<?>[] parameterTypes) {
      return EMPTY;
    }
  }

  private static final Logger LOGGER = getLogger(ReflectiveMethodOperationExecutor.class);
  private static final ArgumentResolverDelegate NO_ARGS_DELEGATE = new NoArgumentsResolverDelegate();

  private final Method method;
  private final Object componentInstance;
  private final ClassLoader extensionClassLoader;

  // Needs to be lazy to wait the muleContext to be injected
  private final LazyValue<ArgumentResolverDelegate> argumentResolverDelegate;

  private MuleContext muleContext;

  public ReflectiveMethodComponentExecutor(List<ParameterGroupModel> groups, Method method, Object componentInstance) {
    this.method = method;
    this.componentInstance = componentInstance;
    argumentResolverDelegate =
        isEmpty(method.getParameterTypes()) ? new LazyValue<>(NO_ARGS_DELEGATE) : getMethodArgumentResolver(groups, method);
    extensionClassLoader = method.getDeclaringClass().getClassLoader();
  }

  private LazyValue<ArgumentResolverDelegate> getMethodArgumentResolver(List<ParameterGroupModel> groups, Method method) {
    return new LazyValue<>(() -> {
      try {
        MethodArgumentResolverDelegate resolver = new MethodArgumentResolverDelegate(groups, method);
        initialiseIfNeeded(resolver, muleContext);
        return resolver;
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not initialize argument resolver resolver"), e);
      }
    });
  }

  public Object execute(ExecutionContext<M> executionContext) {
    return withContextClassLoader(extensionClassLoader,
                                  () -> invokeMethod(method, componentInstance,
                                                     stream(getParameterValues(executionContext, method.getParameterTypes()))
                                                         .map(LazyValue::get).toArray(Object[]::new)));
  }

  private LazyValue<Object>[] getParameterValues(ExecutionContext<M> executionContext, Class<?>[] parameterTypes) {
    return argumentResolverDelegate.get().resolve(executionContext, parameterTypes);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(componentInstance, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(componentInstance);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(componentInstance);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(componentInstance, LOGGER);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
    if (componentInstance instanceof MuleContextAware) {
      ((MuleContextAware) componentInstance).setMuleContext(context);
    }
  }

  @Override
  public Function<ExecutionContext<M>, Map<String, Object>> createArgumentResolver(M operationModel) {
    return ec -> withContextClassLoader(extensionClassLoader,
                                        () -> {
                                          final Object[] resolved =
                                              getParameterValues(ec, method.getParameterTypes());

                                          final Map<String, Object> resolvedParams = new HashMap<>();
                                          for (int i = 0; i < method.getParameterCount(); ++i) {
                                            resolvedParams.put(method.getParameters()[i].getName(), resolved[i]);
                                          }
                                          return resolvedParams;
                                        });
  }
}
