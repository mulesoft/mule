/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FunctionWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.JavaParserUtils;
import org.mule.sdk.api.annotation.ExpressionFunctions;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.createMockLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.setLogger;
import static org.mule.runtime.core.privileged.util.LoggingTestUtils.verifyLogMessage;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.JavaParserUtils.FIRST_MULE_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.slf4j.event.Level.INFO;

public class JavaFunctionModelParserTestCase {

  protected JavaFunctionModelParser parser;
  protected FunctionElement functionElement;

  ClassTypeLoader typeLoader =
      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());

  protected static final String LOGGER_FIELD_NAME = "LOGGER";
  private List<String> infoMessages;
  protected Logger logger;
  private Logger oldLogger;

  @Before
  public void before() throws Exception {
    infoMessages = new ArrayList<>();
    logger = createMockLogger(infoMessages, INFO);
    oldLogger = setLogger(JavaParserUtils.class, LOGGER_FIELD_NAME, logger);
  }

  @After
  public void restoreLogger() throws Exception {
    setLogger(JavaParserUtils.class, LOGGER_FIELD_NAME, oldLogger);
  }

  @Test
  public void getMMVForFunction() throws NoSuchMethodException {
    setParser(Functions.class.getMethod("function"), ConfigurationFunctions.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    Assert.assertThat(minMuleVersion.isPresent(), is(true));
    Assert.assertThat(minMuleVersion.get(), is(FIRST_MULE_VERSION));
    verifyLogMessage(infoMessages,
                     "Function function has min mule version 4.1.1 because it is the default value.");
  }

  @Test
  public void getMMVForSdkAnnotatedFunction() throws NoSuchMethodException {
    setParser(SdkFunctions.class.getMethod("sdkFunction"), ConfigurationFunctions.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    Assert.assertThat(minMuleVersion.isPresent(), is(true));
    Assert.assertThat(minMuleVersion.get().toString(), is("4.5.0"));
    verifyLogMessage(infoMessages,
                     "Method sdkFunction has min mule version 4.5.0 because it is annotated with org.mule.sdk.api.annotation.Alias. org.mule.sdk.api.annotation.Alias has min mule version 4.5.0 because it is annotated with @MinMuleVersion.");
  }

  @Test
  public void getMMVForFunctionFromConfigurationWithSdkFunctionsAnnotation() throws NoSuchMethodException {
    setParser(Functions.class.getMethod("function"), ConfigurationWithSdkFunctionsAnnotation.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    Assert.assertThat(minMuleVersion.isPresent(), is(true));
    Assert.assertThat(minMuleVersion.get().toString(), is("4.5.0"));
    verifyLogMessage(infoMessages,
                     "Function function has min mule version 4.5.0 because it was propagated from the @Functions annotation at the extension class used to add the function.");
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
