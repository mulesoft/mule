/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.test;

import static org.mule.runtime.module.extension.internal.loader.parser.java.test.MinMuleVersionTestUtils.ctxResolvingMinMuleVersion;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FunctionWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaFunctionModelParser;
import org.mule.sdk.api.annotation.ExpressionFunctions;

import java.lang.reflect.Method;

public class JavaFunctionModelParserTestCase {

  protected JavaFunctionModelParser parser;
  protected FunctionElement functionElement;

  ClassTypeLoader typeLoader =
      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());

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
