/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.module.extension.parameter.resolver.AbstractParameterResolverTestCase;
import org.mule.test.parameter.resolver.extension.extension.NestedWrapperTypesConfig;
import org.junit.Test;

import java.io.InputStream;
import java.util.Optional;

public class StackedTypesTestCase extends AbstractParameterResolverTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"nested-wrapper-types.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void configurationWithDynamicParameterResolvers() throws Exception {
    NestedWrapperTypesConfig config = getPayload("configurationWithDynamicParameterResolvers");

    ParameterResolver<TypedValue<KnockeableDoor>> doorResolver = config.getDoorResolver();
    TypedValue<KnockeableDoor> doorTypedValue = doorResolver.resolve();
    KnockeableDoor door = doorTypedValue.getValue();
    assertThat(door.getVictim(), is("Victim's Name"));

    ParameterResolver<TypedValue<InputStream>> lazyParameter = config.getLazyParameter();
    Optional<String> expression = lazyParameter.getExpression();
    TypedValue<InputStream> resolve = lazyParameter.resolve();
    InputStream value = resolve.getValue();
    String stringValue = IOUtils.toString(value);
    assertThat(expression, is(not(empty())));
    assertThat(expression.get(), is("#[output application/json --- {key : 'a nice looking json'}]"));
    assertThat(stringValue, is("{\n  \"key\": \"a nice looking json\"\n}"));

    ParameterResolver<ParameterResolver<ParameterResolver<ParameterResolver<TypedValue<InputStream>>>>> nestedParameter =
        config.getNestedParameter();
    TypedValue<InputStream> nestedTypedValue = nestedParameter.resolve().resolve().resolve().resolve();
    InputStream nestedValue = nestedTypedValue.getValue();
    String nestedString = IOUtils.toString(nestedValue);
    assertThat(nestedString, is("{\n  \"key\": \"pretty nested\"\n}"));

    ParameterResolver<Literal<String>> resolverOfLiteral = config.getResolverOfLiteral();
    Literal<String> stringLiteral = resolverOfLiteral.resolve();
    Optional<String> literalValue = stringLiteral.getLiteralValue();
    assertThat(literalValue, is(not(empty())));
    assertThat(literalValue.get(), is("#['this doesn't make sense']"));
  }

  @Test
  public void configurationWithStaticParameterResolvers() throws Exception {
    NestedWrapperTypesConfig config = getPayload("configurationWithStaticParameterResolvers");

    ParameterResolver<TypedValue<KnockeableDoor>> doorResolver = config.getDoorResolver();
    TypedValue<KnockeableDoor> doorTypedValue = doorResolver.resolve();
    KnockeableDoor door = doorTypedValue.getValue();
    assertThat(door.getVictim(), is("Victim's Name"));

    ParameterResolver<TypedValue<String>> lazyParameter = config.getLazyString();
    Optional<String> expression = lazyParameter.getExpression();
    TypedValue<String> resolve = lazyParameter.resolve();
    assertThat(expression, is(empty()));
    assertThat(resolve.getValue(), is("a nice looking string"));

    ParameterResolver<Literal<String>> resolverOfLiteral = config.getResolverOfLiteral();
    Literal<String> stringLiteral = resolverOfLiteral.resolve();
    Optional<String> literalValue = stringLiteral.getLiteralValue();
    assertThat(literalValue, is(not(empty())));
    assertThat(literalValue.get(), is("this doesn't make sense"));
  }

  @Test
  public void parameterResolverOfTypedValueOnOperation() throws Exception {
    ParameterResolver<TypedValue<InputStream>> lazyValue =
        (ParameterResolver<TypedValue<InputStream>>) flowRunner("parameterResolverOfTypedValueOnOperation").run().getMessage()
            .getPayload().getValue();
    TypedValue<InputStream> resolve = lazyValue.resolve();
    InputStream jsonValue = resolve.getValue();
    Optional<String> expression = lazyValue.getExpression();
    String stringValue = IOUtils.toString(jsonValue);
    assertThat(expression, is(not(empty())));
    assertThat(expression.get(), is("#[output application/json --- {key : 'a nice looking json'}]"));
    assertThat(stringValue, is("{\n  \"key\": \"a nice looking json\"\n}"));
  }
}
