/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.resolver;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;

import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.parameter.resolver.extension.extension.ParameterResolverConfig;
import org.mule.test.parameter.resolver.extension.extension.SomeSource;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(PARAMETERS)
public class ParameterResolverOnConfigTestCase extends AbstractParameterResolverTestCase {

  @Override
  protected String getConfigFile() {
    return "parameter/parameter-resolver-on-pojo-config.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void configurationWithDynamicParameterResolvers() throws Exception {
    ParameterResolverConfig config = getPayload("configurationWithDynamicParameterResolvers");

    org.mule.sdk.api.runtime.parameter.ParameterResolver<KnockeableDoor> doorResolver = config.getDoorResolver();
    ParameterResolver<String> stringResolver = config.getStringResolver();

    assertParameterResolver(doorResolver, of("#[app.registry.staticDoor]"), is(instanceOf(KnockeableDoor.class)));
    assertParameterResolver(stringResolver, of("#[payload]"), is("this is the payload"));
    assertThat(config.getLiteralDoor().getLiteralValue().get(), equalTo("#[aDoor]"));
  }

  @Test
  public void configurationWithStaticParameterResolvers() throws Exception {
    ParameterResolverConfig config = getPayload("configurationWithStaticParameterResolvers");

    org.mule.sdk.api.runtime.parameter.ParameterResolver<KnockeableDoor> doorResolver = config.getDoorResolver();
    ParameterResolver<String> stringResolver = config.getStringResolver();

    assertParameterResolver(doorResolver, empty(), is(instanceOf(KnockeableDoor.class)));
    assertParameterResolver(stringResolver, empty(), is("this is a string"));
  }

  @Test
  public void sourceWithParameterResolver() {
    assertParameterResolver(SomeSource.someString, empty(), is("this is not an expression"));
  }

  @Test
  public void sourceWithLiteral() {
    assertThat(SomeSource.literalString.getLiteralValue().get(), equalTo("#[literal]"));
  }
}
