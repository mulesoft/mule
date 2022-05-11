/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ComposableModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.function.HasFunctionModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.tck.size.SmallTest;

@SmallTest
@RunWith(Parameterized.class)
public class LegacyJsonDeserializationTestCase {

  @Parameterized.Parameter(0)
  public String fileName;

  private String fileContent;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"vegan.json"},
        {"petstore.json"},
        {"metadata.json"},
        {"heisenberg.json"},
        {"substitutiongroup.json"},
        {"tx-ext.json"},
        {"subtypes.json"},
        {"marvel.json"},
        {"ram.json"},
        {"typed-value.json"},
        {"test-oauth.json"},
        {"test-oauth-ocs.json"},
        {"test-fn.json"},
        {"values.json"},
        {"implicit-config.json"},
        {"non-implicit-config.json"},
        {"reconnection-extension.json"},
    });
  }

  @Before
  public void setup() throws IOException {
    fileContent = getResourceAsString(getExpectedFilesDir() + fileName, getClass());
  }

  private final ExtensionModelJsonSerializer generator = new ExtensionModelJsonSerializer(true);

  protected String getExpectedFilesDir() {
    return "models/legacy/";
  }

  @Test
  public void load() {
    ExtensionModel result = generator.deserialize(fileContent);
    assertLegacyExtensionModelCollections(result);
  }

  private void assertLegacyExtensionModelCollections(ExtensionModel result) {
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        assertParameterizedModel(model);
        assertEnrichableModel(model);
        assertNotNull(model.getOperationModels());
        assertNotNull(model.getConnectionProviders());
        assertNotNull(model.getExternalLibraryModels());
        assertNotNull(model.getSourceModels());
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        assertComponentModel(model);
        assertNotNull(model.getNotificationModels());
        assertSampleDataProviderModel(model);
      }

      @Override
      protected void onFunction(HasFunctionModels owner, FunctionModel model) {
        assertParameterizedModel(model);
        assertEnrichableModel(model);
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        assertComponentModel(model);
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        assertParameterizedModel(model);
        assertEnrichableModel(model);
        assertNotNull(model.getExternalLibraryModels());
        assertNotNull(model.getSemanticTerms());
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        assertComponentModel(model);
        assertSampleDataProviderModel(model);
        assertNotNull(model.getNotificationModels());
        model.getErrorCallback().ifPresent(LegacyJsonDeserializationTestCase.this::assertSourceCallbackModel);
        model.getSuccessCallback().ifPresent(LegacyJsonDeserializationTestCase.this::assertSourceCallbackModel);
        model.getTerminateCallback().ifPresent(LegacyJsonDeserializationTestCase.this::assertSourceCallbackModel);
      }

      @Override
      protected void onParameterGroup(ParameterizedModel owner, ParameterGroupModel model) {
        assertEnrichableModel(model);
        assertNotNull(model.getParameterModels());
        assertNotNull(model.getExclusiveParametersModels());
      }

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        assertEnrichableModel(model);
        assertNotNull(model.getAllowedStereotypes());
        assertNotNull(model.getSemanticTerms());
        assertFieldValueProviderModels(model);
        model.getValueProviderModel().ifPresent(valueProviderModel -> valueProviderModel.getParameters());
      }

      @Override
      protected void onNestable(ComposableModel owner, NestableElementModel model) {
        assertComponentModel(model);
      }
    }.walk(result);
  }

  public void assertParameterizedModel(ParameterizedModel model) {
    assertNotNull(model.getAllParameterModels());
    assertNotNull(model.getParameterGroupModels());
  }

  public void assertEnrichableModel(EnrichableModel model) {
    assertNotNull(model.getModelProperties());
  }

  public void assertComponentModel(ComponentModel model) {
    assertParameterizedModel(model);
    assertEnrichableModel(model);
    assertNotNull(model.getErrorModels());
    assertNotNull(model.getNestedComponents());
    assertNotNull(model.getSemanticTerms());
  }

  private void assertSampleDataProviderModel(HasOutputModel model) {
    assertNotNull(model.getSampleDataProviderModel());
    model.getSampleDataProviderModel().ifPresent(sampleDataProviderModel -> {
      assertNotNull(sampleDataProviderModel.getParameters());
    });
  }

  private void assertSourceCallbackModel(SourceCallbackModel sourceCallbackModel) {
    assertParameterizedModel(sourceCallbackModel);
    assertEnrichableModel(sourceCallbackModel);
  }

  private void assertFieldValueProviderModels(ParameterModel model) {
    assertNotNull(model.getFieldValueProviderModels());
    model.getFieldValueProviderModels().forEach(fieldValueProviderModel -> assertValueProviderModel(fieldValueProviderModel));
  }

  private void assertValueProviderModel(ValueProviderModel model) {
    assertEnrichableModel(model);
    assertNotNull(model.getParameters());
  }

  public void assertNotNull(Object value) {
    assertThat(value, is(notNullValue()));
  }

}
