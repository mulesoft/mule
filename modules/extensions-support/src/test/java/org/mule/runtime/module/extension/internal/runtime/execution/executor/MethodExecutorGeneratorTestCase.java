/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.runtime.module.extension.internal.runtime.execution.executor.MethodExecutorGeneratorTestCase.Utils.mockArgumentResolverDelegate;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.internal.runtime.execution.ArgumentResolverDelegate;
import org.mule.runtime.module.extension.internal.runtime.execution.GeneratedInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

public class MethodExecutorGeneratorTestCase {

  private MethodExecutorGenerator generator = new MethodExecutorGenerator();


  /**
   * Utilities for debugging purposes. Not meant to use during test run
   */
  public static class Utils {

    public static File copyGeneratedClass(MethodExecutorGenerator generator,
                                          Object target,
                                          Method method,
                                          File targetDirectory)
        throws Exception {
      GeneratedInstance<MethodExecutor> generatedInstance =
          generator.generate(target, method, mockArgumentResolverDelegate(method));
      File byteCodeFile = generatedInstance.getGeneratedClass().getByteCodeFile();
      File targetFile = new File(targetDirectory, byteCodeFile.getName());
      copyFile(byteCodeFile, targetFile, true);

      return targetFile;
    }

    public static ArgumentResolverDelegate mockArgumentResolverDelegate(Method method) {
      ArgumentResolverDelegate delegate = mock(ArgumentResolverDelegate.class);
      ArgumentResolver[] resolvers = new ArgumentResolver[method.getParameterCount()];
      for (int i = 0; i < method.getParameterCount(); i++) {
        resolvers[i] = mock(ArgumentResolver.class);
      }

      when(delegate.getArgumentResolvers()).thenReturn(resolvers);
      return delegate;
    }
  }

  @Test
  public void sameMethodGeneratesUniqueClass() throws Exception {
    Method method = getMethod("sampleOperation");
    GeneratedInstance<MethodExecutor> generatedInstance1 = generator.generate(this, method, mockArgumentResolverDelegate(method));
    GeneratedInstance<MethodExecutor> generatedInstance2 = generator.generate(this, method, mockArgumentResolverDelegate(method));

    assertThat(generatedInstance1.getInstance(), is(notNullValue()));
    assertThat(generatedInstance2.getInstance(), is(notNullValue()));
    assertThat(generatedInstance1.getInstance(), is(not(sameInstance(generatedInstance2.getInstance()))));

    assertThat(generatedInstance1.getGeneratedClass().getGeneratedClass(),
               is(sameInstance(generatedInstance2.getGeneratedClass().getGeneratedClass())));

    assertThat(generatedInstance1.getInstance().getClass(),
               is(sameInstance(generatedInstance1.getGeneratedClass().getGeneratedClass())));
    assertThat(generatedInstance2.getInstance().getClass(),
               is(sameInstance(generatedInstance2.getGeneratedClass().getGeneratedClass())));
  }

  @Test
  public void differentMethodsGenerateDifferentClasses() throws Exception {
    Method method1 = getMethod("sampleOperation");
    Method method2 = getMethod("anotherOperation");

    GeneratedInstance<MethodExecutor> generatedInstance1 =
        generator.generate(this, method1, mockArgumentResolverDelegate(method1));
    GeneratedInstance<MethodExecutor> generatedInstance2 =
        generator.generate(this, method2, mockArgumentResolverDelegate(method2));

    assertThat(generatedInstance1.getInstance(), is(notNullValue()));
    assertThat(generatedInstance2.getInstance(), is(notNullValue()));
    assertThat(generatedInstance1.getInstance(), is(not(sameInstance(generatedInstance2.getInstance()))));

    assertThat(generatedInstance1.getGeneratedClass().getGeneratedClass(),
               is(not(sameInstance(generatedInstance2.getGeneratedClass().getGeneratedClass()))));

    assertThat(generatedInstance1.getInstance().getClass(),
               is(sameInstance(generatedInstance1.getGeneratedClass().getGeneratedClass())));
    assertThat(generatedInstance2.getInstance().getClass(),
               is(sameInstance(generatedInstance2.getGeneratedClass().getGeneratedClass())));
  }

  private Method getMethod(String methodName) {
    return Stream.of(getClass().getMethods()).filter(m -> m.getName().equals(methodName)).findFirst().get();
  }

  public InputStream sampleOperation(@Config MethodExecutorGeneratorTestCase config,
                                     String param1,
                                     Map<String, Object> map,
                                     StreamingHelper streamingHelper) {
    return new ByteArrayInputStream(param1.getBytes());
  }

  public String anotherOperation(@Config MethodExecutorGeneratorTestCase config, int param1) {
    return "" + param1;
  }
}
