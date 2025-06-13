/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.PARAMETERS;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.extension.api.declaration.type.annotation.TypedValueTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.metadata.api.utils.MetadataTypeEnricher;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Story(PARAMETERS)
public class MuleSdkParameterModelParserTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = none();

  private MuleSdkParameterModelParserSdkBuilder baseParameterParserBuilder;
  private MetadataType someValidMetadataType;

  @Before
  public void setUp() {
    someValidMetadataType = BaseTypeBuilder.create(JAVA).anyType().build();
    baseParameterParserBuilder = new MuleSdkParameterModelParserSdkBuilder("someparam", "somevalid")
        .withTypeLoaderTypes(singletonMap("somevalid", someValidMetadataType));
  }

  // ------------------------------- //
  // Parameter Type
  // ------------------------------- //

  @Test
  public void invalidParameterTypeRaisesException() {
    expected.expect(IllegalModelDefinitionException.class);
    expected.expectMessage("Parameter 'someparam' references unknown type 'invalid'");
    baseParameterParserBuilder.withType("invalid").build();
  }

  @Test
  public void parameterTypeCanNotBeVoid() {
    expected.expect(IllegalModelDefinitionException.class);
    expected.expectMessage("Parameter 'someparam' references type 'void', which is forbidden for parameters");
    baseParameterParserBuilder.withType("void").build();
  }

  @Test
  public void parameterTypeCanBeSomeValidParameterInTheApplicationTypeLoader() {
    MuleSdkParameterModelParser parameterModelParser = baseParameterParserBuilder.withType("somevalid").build();
    MetadataType expectedType =
        new MetadataTypeEnricher().enrich(someValidMetadataType, singleton(new TypedValueTypeAnnotation()));
    assertThat(parameterModelParser.getType(), is(expectedType));
  }

  // ------------------------------- //
  // Deprecation
  // ------------------------------- //

  @Test
  public void when_parameterAstHasNotDeprecationParameter_then_parserHasNotDeprecationModel() {
    MuleSdkParameterModelParser parameterModelParser = baseParameterParserBuilder.build();
    assertThat(parameterModelParser.getDeprecationModel().isPresent(), is(false));
  }

  @Test
  public void when_parameterAstHasDeprecationParameter_then_parserHasDeprecationModelWithCorrespondingValues() {
    MuleSdkParameterModelParser parameterModelParser = baseParameterParserBuilder
        .withDeprecationModel("1.1.0", "Some Message", "2.0.0")
        .build();

    assertThat(parameterModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = parameterModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getDeprecatedSince(), is("1.1.0"));
    assertThat(deprecationModel.getMessage(), is("Some Message"));
    assertThat(deprecationModel.getToRemoveIn(), is(Optional.of("2.0.0")));
  }

  @Test
  public void when_toRemoveInParameterIsNotConfigured_then_theDeprecationModelReturnsAnEmptyOptional() {
    MuleSdkParameterModelParser parameterModelParser = baseParameterParserBuilder
        .withDeprecationModel("1.1.0", "Some Message", null)
        .build();

    assertThat(parameterModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = parameterModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getToRemoveIn(), is(empty()));
  }
}
