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
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FunctionWrapper;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class JavaFunctionModelParserTestCase {

  private JavaFunctionModelParser parser;
  private FunctionElement functionElement;
  private ExtensionElement extensionElement;

  ClassTypeLoader typeLoader =
      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());

  @Test
  public void getMinMuleVersionFunction() throws NoSuchMethodException {
    functionElement = new FunctionWrapper(Functions.class.getMethod("function"), typeLoader);
    parser = new JavaFunctionModelParser(mock(ExtensionElement.class), functionElement, mock(ExtensionLoadingContext.class));
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    Assert.assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    Assert.assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.1.0"));
  }

  @Test
  public void getMinMuleVersionSdkFunction() throws NoSuchMethodException {
    functionElement = new FunctionWrapper(SdkFunctions.class.getMethod("sdkFunction"), typeLoader);
    parser = new JavaFunctionModelParser(mock(ExtensionElement.class), functionElement, mock(ExtensionLoadingContext.class));
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    Assert.assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    Assert.assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.5.0"));
  }

  private class Functions {

    public void function() {}
  }

  private class SdkFunctions {

    @org.mule.sdk.api.annotation.Alias("alias")
    public void sdkFunction() {}
  }
}
