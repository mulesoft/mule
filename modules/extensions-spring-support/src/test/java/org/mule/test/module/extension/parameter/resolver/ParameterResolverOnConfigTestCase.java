/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.resolver;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.parameter.resolver.extension.extension.ParameterResolverExtension;
import org.mule.test.parameter.resolver.extension.extension.SomeSource;

import org.junit.Test;

public class ParameterResolverOnConfigTestCase extends AbstractParameterResolverTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"parameter-resolver-on-pojo-config.xml"};
  }

  @Test
  public void configurationWithDynamicParameterResolvers() throws Exception {
    ParameterResolverExtension config = getPayload("configurationWithDynamicParameterResolvers");

    ParameterResolver<KnockeableDoor> doorResolver = config.getDoorResolver();
    ParameterResolver<String> stringResolver = config.getStringResolver();

    assertParameterResolver(doorResolver, of("#[app.registry.staticDoor]"), is(instanceOf(KnockeableDoor.class)));
    assertParameterResolver(stringResolver, of("#[payload]"), is("this is the payload"));
  }

  @Test
  public void configurationWithDynamicParameterResolversWithDynamicPojo() throws Exception {
    ParameterResolverExtension config = getPayload("configurationWithDynamicParameterResolversWithDynamicPojo");

    ParameterResolver<KnockeableDoor> doorResolver = config.getDoorResolver();
    ParameterResolver<String> stringResolver = config.getStringResolver();
    doorResolver.resolve();

    assertParameterResolver(doorResolver, of("#[app.registry.dynamicDoor]"), is(instanceOf(KnockeableDoor.class)));
    assertParameterResolver(stringResolver, of("#[payload]"), is("this is the payload"));
  }

  @Test
  public void configurationWithStaticParameterResolvers() throws Exception {
    ParameterResolverExtension config = getPayload("configurationWithStaticParameterResolvers");

    ParameterResolver<KnockeableDoor> doorResolver = config.getDoorResolver();
    ParameterResolver<String> stringResolver = config.getStringResolver();

    assertParameterResolver(doorResolver, empty(), is(instanceOf(KnockeableDoor.class)));
    assertParameterResolver(stringResolver, empty(), is("this is a string"));
  }

  @Test
  public void sourceWithParameterResolver() throws Exception {
    assertParameterResolver(SomeSource.someString, empty(), is("this is not an expression"));
  }
}
