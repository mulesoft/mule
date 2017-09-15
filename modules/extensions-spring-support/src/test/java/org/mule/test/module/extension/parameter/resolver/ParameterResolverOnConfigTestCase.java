/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.parameter.resolver.extension.extension.ParameterResolverConfig;
import org.mule.test.parameter.resolver.extension.extension.SomeSource;
import org.junit.Test;

public class ParameterResolverOnConfigTestCase extends AbstractParameterResolverTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"parameter-resolver-on-pojo-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void configurationWithDynamicParameterResolvers() throws Exception {
    ParameterResolverConfig config = getPayload("configurationWithDynamicParameterResolvers");

    ParameterResolver<KnockeableDoor> doorResolver = config.getDoorResolver();
    ParameterResolver<String> stringResolver = config.getStringResolver();

    assertParameterResolver(doorResolver, of("#[app.registry.staticDoor]"), is(instanceOf(KnockeableDoor.class)));
    assertParameterResolver(stringResolver, of("#[payload]"), is("this is the payload"));
    assertThat(config.getLiteralDoor().getLiteralValue().get(), equalTo("#[aDoor]"));
  }

  @Test
  public void configurationWithStaticParameterResolvers() throws Exception {
    ParameterResolverConfig config = getPayload("configurationWithStaticParameterResolvers");

    ParameterResolver<KnockeableDoor> doorResolver = config.getDoorResolver();
    ParameterResolver<String> stringResolver = config.getStringResolver();

    assertParameterResolver(doorResolver, empty(), is(instanceOf(KnockeableDoor.class)));
    assertParameterResolver(stringResolver, empty(), is("this is a string"));
  }

  @Test
  public void sourceWithParameterResolver() throws Exception {
    assertParameterResolver(SomeSource.someString, empty(), is("this is not an expression"));
  }

  @Test
  public void sourceWithLiteral() throws Exception {
    assertThat(SomeSource.literalString.getLiteralValue().get(), equalTo("#[literal]"));
  }
}
