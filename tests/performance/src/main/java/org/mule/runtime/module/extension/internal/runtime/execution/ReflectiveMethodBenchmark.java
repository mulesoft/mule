/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.executor.MethodExecutor;
import org.mule.runtime.module.extension.internal.runtime.execution.executor.MethodExecutorGenerator;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Threads;

@Threads(3)
public class ReflectiveMethodBenchmark extends AbstractBenchmark {

  private static class Target {

    public int doIt(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9) {
      return arg0 + arg1 + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8 + arg9;

    }
  }

  private Target target;
  private Method method;

  @Setup
  public void setUp() throws NoSuchMethodException, SecurityException {
    target = new Target();
    method =
        target.getClass().getDeclaredMethod("doIt", new Class[] {int.class, int.class, int.class, int.class, int.class, int.class,
            int.class, int.class, int.class, int.class});
  }

  @Benchmark
  public Object reflectionCall() {
    try {
      return method.invoke(target, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Benchmark
  public Object generated() throws Exception {
    ArgumentResolverDelegate resolverDelegate = new ArgumentResolverDelegate() {

      private static final int COUNT = 10;

      @Override
      public ArgumentResolver<?>[] getArgumentResolvers() {
        ArgumentResolver<?>[] resolvers = new ArgumentResolver[COUNT];
        for (int i = 0; i < COUNT; i++) {
          int retVal = i;
          resolvers[i] = (ArgumentResolver<Object>) executionContext -> retVal;
        }

        return resolvers;
      }

      @Override
      public Object[] resolve(ExecutionContext executionContext, Class<?>[] parameterTypes) {
        Object[] values = new Object[COUNT];
        for (int i = 0; i < COUNT; i++) {
          values[i] = i;
        }

        return values;
      }

      @Override
      public Supplier<Object>[] resolveDeferred(ExecutionContext executionContext, Class<?>[] parameterTypes) {
        Supplier<Object>[] suppliers = new Supplier[COUNT];
        for (int i = 0; i < COUNT; i++) {
          int retVal = i;
          suppliers[i] = () -> retVal;
        }

        return suppliers;
      }
    };

    MethodExecutor executor = new MethodExecutorGenerator().generate(target, method, resolverDelegate);
    return executor.execute(new ExecutionContext() {

      @Override
      public boolean hasParameter(String parameterName) {
        return false;
      }

      @Override
      public Object getParameter(String parameterName) {
        return null;
      }

      @Override
      public Object getParameterOrDefault(String parameterName, Object defaultValue) {
        return null;
      }

      @Override
      public Map<String, Object> getParameters() {
        return null;
      }

      @Override
      public Optional<ConfigurationInstance> getConfiguration() {
        return Optional.empty();
      }

      @Override
      public ExtensionModel getExtensionModel() {
        return null;
      }

      @Override
      public ComponentModel getComponentModel() {
        return null;
      }
    });
  }
}
