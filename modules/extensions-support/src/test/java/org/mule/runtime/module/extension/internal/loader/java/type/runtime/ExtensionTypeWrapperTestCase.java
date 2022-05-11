/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.sdk.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Extension;

import java.util.List;

import org.junit.Test;

public class ExtensionTypeWrapperTestCase {

  @Test
  public void getConfigurationsFromExtensionWithConfigurationsUsingTheSkApiAndTheLegacyApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);

    ExtensionTypeWrapper extensionTypeWrapper = new ExtensionTypeWrapper<>(SimpleSdkExtension.class, typeLoader);
    List<ConfigurationElement> configurationElements = extensionTypeWrapper.getConfigurations();

    assertThat(configurationElements.size(), is(2));
    assertThat(configurationElements.get(0).getDeclaringClass().get(), equalTo(SimpleSdkConfiguration.class));
    assertThat(configurationElements.get(1).getDeclaringClass().get(), equalTo(SimpleLegacyConfiguration.class));
  }

  @Extension(name = "SimpleSdkExtension")
  @Configurations({SimpleSdkConfiguration.class, SimpleLegacyConfiguration.class})
  private static class SimpleSdkExtension {
  }

  @Configuration(name = "newSdkConfiguration")
  private static class SimpleSdkConfiguration {
  }


  @org.mule.runtime.extension.api.annotation.Configuration(name = "oldLegacyConfiguration")
  private static class SimpleLegacyConfiguration {
  }
}
