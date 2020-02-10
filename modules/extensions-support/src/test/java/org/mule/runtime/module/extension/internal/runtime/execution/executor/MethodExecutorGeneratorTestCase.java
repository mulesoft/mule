/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.FileUtils.TEMP_DIR;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.internal.runtime.execution.ArgumentResolverDelegate;
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

  @Test
  public void generateNonPrimitives() throws Exception {
    Method method = getMethod("noPrimitivesOperation");
    generator.generate(this, method, mockArgumentResolverDelegate(method));

    getByteCodeFile(method);
  }


  private File getByteCodeFile(Method method) throws Exception {
    String generatorName = method.getDeclaringClass().getName() + "$" + method.getName() + "$1$MethodComponentExecutorWrapper";
    final File source = new File(TEMP_DIR, generatorName + ".class");
    assertThat(source.exists(), is(true));

    File target = new File("/Users/mariano.gonzalez/Desktop", source.getName());
    copyFile(source, target, true);

    return target;
  }

  private Method getMethod(String methodName) {
    return Stream.of(getClass().getMethods()).filter(m -> m.getName().equals(methodName)).findFirst().get();
  }

  private ArgumentResolverDelegate mockArgumentResolverDelegate(Method method) {
    ArgumentResolverDelegate delegate = mock(ArgumentResolverDelegate.class);
    ArgumentResolver[] resolvers = new ArgumentResolver[method.getParameterCount()];
    for (int i = 0; i < method.getParameterCount(); i++) {
      resolvers[i] = mock(ArgumentResolver.class);
    }

    when(delegate.getArgumentResolvers()).thenReturn(resolvers);
    return delegate;
  }

  public InputStream noPrimitivesOperation(@Config MethodExecutorGeneratorTestCase config,
                                           String param1,
                                           Map<String, Object> map,
                                           StreamingHelper streamingHelper) {
    return new ByteArrayInputStream(param1.getBytes());
  }
}
