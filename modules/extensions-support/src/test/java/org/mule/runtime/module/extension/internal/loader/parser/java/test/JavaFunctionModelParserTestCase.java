/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.test;

import static org.mule.runtime.module.extension.internal.loader.parser.java.test.MinMuleVersionTestUtils.ctxResolvingMinMuleVersion;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion.FIRST_MULE_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FunctionWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaFunctionModelParser;
import org.mule.sdk.api.annotation.ExpressionFunctions;

import java.lang.reflect.Method;

import org.junit.Test;

public class JavaFunctionModelParserTestCase {

  protected JavaFunctionModelParser parser;
  protected FunctionElement functionElement;

  ClassTypeLoader typeLoader =
      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());

  @Test
  public void getMMVForFunction() throws NoSuchMethodException {
    setParser(Functions.class.getMethod("function"), ConfigurationFunctions.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Function function has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVForSdkAnnotatedFunction() throws NoSuchMethodException {
    setParser(SdkFunctions.class.getMethod("sdkFunction"), ConfigurationFunctions.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method sdkFunction has min mule version 4.5 because it is annotated with Alias. Alias was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForFunctionFromConfigurationWithSdkFunctionsAnnotation() throws NoSuchMethodException {
    setParser(Functions.class.getMethod("function"), ConfigurationWithSdkFunctionsAnnotation.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Function function has min mule version 4.5 because it was propagated from the @Functions annotation at the extension class used to add the function."));
  }

  protected void setParser(Method method, Class<?> extensionClass) {
    functionElement = new FunctionWrapper(method, typeLoader);
    parser = new JavaFunctionModelParser(new ExtensionTypeWrapper<>(extensionClass, TYPE_LOADER), functionElement,
                                         ctxResolvingMinMuleVersion());
  }

  private class Functions {

    public void function() {}
  }

  private class SdkFunctions {

    @org.mule.sdk.api.annotation.Alias("alias")
    public void sdkFunction() {}
  }

  @org.mule.runtime.extension.api.annotation.ExpressionFunctions({Functions.class, SdkFunctions.class})
  private class ConfigurationFunctions {
  }

  @ExpressionFunctions(Functions.class)
  private class ConfigurationWithSdkFunctionsAnnotation {
  }
}
