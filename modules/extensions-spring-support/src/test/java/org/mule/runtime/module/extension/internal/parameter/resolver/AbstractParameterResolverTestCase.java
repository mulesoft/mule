/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.parameter.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matcher;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
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
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

public abstract class AbstractParameterResolverTestCase extends ExtensionFunctionalTestCase {

  protected <T> T getPayload(String flowName) throws Exception {
    return (T) flowRunner(flowName).run().getMessage().getPayload().getValue();
  }

  protected void assertParameterResolver(ParameterResolver resolver, java.util.Optional<String> expression, Matcher value) {
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

    public DifferedKnockableDoor resolverOperation(DifferedKnockableDoor differedDoor) {
      return differedDoor;
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
