/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

public class ParameterResolverOnPojoTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {ParameterResolverExtension.class};
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"parameter-resolver-on-pojo-config.xml"};
  }

  @Before
  public void setUp(){
    SomeSource.someString = null;
    SomeSource.extension = null;
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
  public void operationWithChildElement() throws Exception {
    PojoWithParameterResolverParameter pojo = getPayload("operationWithChildElement");

    ParameterResolver<String> someExpression = pojo.getSomeExpression();
    assertParameterResolver(someExpression, of("#[payload]"), is("this is the payload"));
  }

  @Test
  public void operationWithDynamicReferenceElement() throws Exception {
    PojoWithParameterResolverParameter pojo = getPayload("operationWithDynamicReferenceElement");

    ParameterResolver<String> someExpression = pojo.getSomeExpression();
    assertParameterResolver(someExpression, of("#[payload]"), is("this is the payload"));
  }


  @Test
  public void operationWithStaticReferenceElement() throws Exception {
    PojoWithParameterResolverParameter pojo = getPayload("operationWithStaticReferenceElement");

    ParameterResolver<String> someExpression = pojo.getSomeExpression();
    assertParameterResolver(someExpression, empty(), is("this is not an expression"));
  }

  @Test
  public void sourceWithParameterResolver() throws Exception {
    assertParameterResolver(SomeSource.someString, empty(), is("this is not an expression"));
  }

  private <T> T getPayload(String flowName) throws Exception {
    return (T) flowRunner(flowName).run().getMessage().getPayload().getValue();
  }

  private void assertParameterResolver(ParameterResolver resolver, java.util.Optional<String> expression, Matcher value) {
    assertThat(resolver.getExpression(), is(expression));
    assertThat(resolver.resolve(), value);
  }

  @Operations(ParameterResolverParameterOperations.class)
  @Extension(name = "ParameterResolver")
  @Sources(SomeSource.class)
  public static class ParameterResolverExtension {

    public ParameterResolver<String> getStringResolver() {
      return stringResolver;
    }

    public void setStringResolver(ParameterResolver<String> stringResolver) {
      this.stringResolver = stringResolver;
    }

    public ParameterResolver<KnockeableDoor> getDoorResolver() {
      return doorResolver;
    }

    public void setDoorResolver(ParameterResolver<KnockeableDoor> doorResolver) {
      this.doorResolver = doorResolver;
    }

    @Parameter
    @Optional
    ParameterResolver<String> stringResolver;

    @Parameter
    @Optional
    @XmlHints(allowTopLevelDefinition = true)
    ParameterResolver<KnockeableDoor> doorResolver;

  }

  public static class ParameterResolverParameterOperations {

    public ParameterResolverExtension configOperation(@UseConfig ParameterResolverExtension config) {
      return config;
    }

    public PojoWithParameterResolverParameter resolverOperation(PojoWithParameterResolverParameter pojoWithResolver) {
      return pojoWithResolver;
    }

    public ParameterResolver<KnockeableDoor> doorOperation(@Optional ParameterResolver<KnockeableDoor> door,
                                                           @Optional KnockeableDoor someDoor) {
      return door;
    }

    public ParameterResolver<String> stringOperation(@Optional(
        defaultValue = "this is a string") ParameterResolver<String> string) {
      return string;
    }
  }

  @XmlHints(allowTopLevelDefinition = true)
  public static class PojoWithParameterResolverParameter {

    public ParameterResolver<String> getSomeExpression() {
      return someExpression;
    }

    @Parameter
    ParameterResolver<String> someExpression;
  }

  public static class SomeSource extends Source<String, Attributes> {

    @Parameter
    public static ParameterResolver<String> someString;

    @UseConfig
    public static ParameterResolverExtension extension;

    @Override
    public void onStart(SourceCallback<String, Attributes> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {

    }
  }
}
