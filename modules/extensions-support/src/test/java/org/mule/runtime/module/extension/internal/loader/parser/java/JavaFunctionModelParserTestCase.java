/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.junit.Assert;
import org.junit.Test;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FunctionWrapper;
import org.mule.sdk.api.annotation.ExpressionFunctions;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.meta.MuleVersion.FIRST_MULE_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

public class JavaFunctionModelParserTestCase {

  protected JavaFunctionModelParser parser;
  protected FunctionElement functionElement;

  ClassTypeLoader typeLoader =
      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());

  @Test
  public void getMMVForFunction() throws NoSuchMethodException {
    setParser(Functions.class.getMethod("function"), ConfigurationFunctions.class);
    Assert.assertThat(parser.getMinMuleVersionResult().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Function function has min mule version 4.1.1 because it is the default value."));
  }

  @Test
  public void getMMVForSdkAnnotatedFunction() throws NoSuchMethodException {
    setParser(SdkFunctions.class.getMethod("sdkFunction"), ConfigurationFunctions.class);
    Assert.assertThat(parser.getMinMuleVersionResult().getMinMuleVersion().toString(), is("4.5.0"));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Method sdkFunction has min mule version 4.5.0 because it is annotated with org.mule.sdk.api.annotation.Alias. org.mule.sdk.api.annotation.Alias has min mule version 4.5.0 because it is annotated with @MinMuleVersion."));
  }

  @Test
  public void getMMVForFunctionFromConfigurationWithSdkFunctionsAnnotation() throws NoSuchMethodException {
    setParser(Functions.class.getMethod("function"), ConfigurationWithSdkFunctionsAnnotation.class);
    Assert.assertThat(parser.getMinMuleVersionResult().getMinMuleVersion().toString(), is("4.5.0"));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Function function has min mule version 4.5.0 because it was propagated from the @Functions annotation at the extension class used to add the function."));
  }

  protected void setParser(Method method, Class<?> extensionClass) {
    functionElement = new FunctionWrapper(method, typeLoader);
    parser = new JavaFunctionModelParser(new ExtensionTypeWrapper<>(extensionClass, TYPE_LOADER), functionElement,
                                         mock(ExtensionLoadingContext.class));
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
