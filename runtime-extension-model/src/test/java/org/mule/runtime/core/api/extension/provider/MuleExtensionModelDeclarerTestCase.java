/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider;

import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.OUTPUT;
import static org.mule.runtime.api.util.MuleSystemProperties.PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE;
import static org.mule.runtime.core.extension.ComponentConfigurerTestUtils.createMockedFactory;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;

import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@RunWith(Parameterized.class)
public class MuleExtensionModelDeclarerTestCase {

  private String isParseTemplateUseLegacyDefaultTargetValueOriginalValue;
  private final String isParseTemplateUseLegacyDefaultTargetValue;

  @Parameterized.Parameters(
      name = "Use parseTemplate legacy default target value: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"true"},
        {"false"}
    });
  }

  public MuleExtensionModelDeclarerTestCase(String isParseTemplateUseLegacyDefaultTargetValue) {
    this.isParseTemplateUseLegacyDefaultTargetValue = isParseTemplateUseLegacyDefaultTargetValue;
  }

  // TODO W-13974116: Remove circular dependency when adding org.mule.tests:mule-tests-unit to this module
  @Before
  public void setUp() {
    isParseTemplateUseLegacyDefaultTargetValueOriginalValue =
        setProperty(PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE, isParseTemplateUseLegacyDefaultTargetValue);
  }

  @After
  public void tearDown() {
    if (isParseTemplateUseLegacyDefaultTargetValueOriginalValue == null) {
      clearProperty(PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE);
    } else {
      setProperty(PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE, isParseTemplateUseLegacyDefaultTargetValueOriginalValue);
    }
  }

  @Test
  @Issue("W-13965819")
  public void whenIsParseTemplateUseLegacyDefaultTargetValueTheTargetValueIsMessage() {
    // We want to assert over the ExtensionModel and not the ExtensionDeclarer, because the enrichers may be doing overrides that
    // we don't expect
    // We also can't use MuleExtensionModelProvider#getExtensionModel because that one caches the result in the class and we need
    // to recreate it with and without the property
    ExtensionModel muleExtensionModel = new TestExtensionModelLoader()
        .loadExtensionModel(new MuleExtensionModelDeclarer(createMockedFactory()).createExtensionModel(), loadingRequest());
    OperationModel parseTemplateOperation = muleExtensionModel.getOperationModel("parseTemplate").get();
    assertTargetValueParameter(parseTemplateOperation);
  }

  private void assertTargetValueParameter(OperationModel parseTemplateOperation) {
    ParameterModel targetValueParam = parseTemplateOperation.getParameterGroupModels().stream()
        .filter(op -> op.getName().equals(OUTPUT))
        .findFirst()
        .flatMap(pgm -> pgm.getParameter(TARGET_VALUE_PARAMETER_NAME))
        .get();

    if (parseBoolean(isParseTemplateUseLegacyDefaultTargetValue)) {
      assertThat(targetValueParam.getDefaultValue(), equalTo("#[message]"));
    } else {
      assertThat(targetValueParam.getDefaultValue(), equalTo("#[payload]"));
    }
  }

  private static ExtensionModelLoadingRequest loadingRequest() {
    return builder(MuleExtensionModelProvider.class.getClassLoader(), nullDslResolvingContext()).build();
  }

  private static final class TestExtensionModelLoader extends ExtensionModelLoader {

    @Override
    protected void declareExtension(ExtensionLoadingContext context) {
      // nothing to do
    }

    @Override
    public String getId() {
      return "test";
    }
  }
}
