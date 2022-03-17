/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase.ResourceExtensionUnitTest.newUnitTest;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ComposableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
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
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.marvel.MarvelExtension;

import org.skyscreamer.jsonassert.JSONAssert;

@SmallTest
@RunWith(Parameterized.class)
public class LegacyJsonDeserializationTestCase extends FileGenerationParameterizedExtensionModelTestCase {

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    List<ResourceExtensionUnitTest> extensions;
    extensions = asList(
                        newUnitTest(JAVA_LOADER, MarvelExtension.class, "marvel-old.json"));

    return createExtensionModels(extensions);
  }

  private final ExtensionModelJsonSerializer generator = new ExtensionModelJsonSerializer(true);

  @Override
  protected String getExpectedFilesDir() {
    return "models/";
  }

  @Override
  protected String doGenerate(ExtensionModel extensionUnderTest) throws Exception {
    return generator.serialize(extensionUnderTest).trim();
  }

  @Override
  protected void assertEquals(String expectedContent, String actualContent) throws Exception {
    JSONAssert.assertEquals(expectedContent, actualContent, true);
  }

  @Test
  public void load() {
    ExtensionModel result = generator.deserialize(expectedContent);
    assertLegacyExtensionModelCollections(result);
  }

  private void assertLegacyExtensionModelCollections(ExtensionModel result) {
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        assertParameterizedModel(model);
        model.getOperationModels();
        model.getConnectionProviders();
        model.getExternalLibraryModels();
        model.getModelProperties();
        model.getSourceModels();
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        assertParameterizedModel(model);
        model.getModelProperties();
        model.getErrorModels();
        model.getNestedComponents();
        model.getNotificationModels();
        model.getSemanticTerms();
        model.getSampleDataProviderModel().ifPresent(sampleDataProviderModel -> {
          sampleDataProviderModel.getParameters();
        });
      }

      @Override
      protected void onFunction(HasFunctionModels owner, FunctionModel model) {
        assertParameterizedModel(model);
        model.getModelProperties();
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        assertParameterizedModel(model);
        model.getModelProperties();
        model.getErrorModels();
        model.getNestedComponents();
        model.getSemanticTerms();
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        assertParameterizedModel(model);
        model.getModelProperties();
        model.getExternalLibraryModels();
        model.getSemanticTerms();
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        assertParameterizedModel(model);
        model.getModelProperties();
        model.getErrorModels();
        model.getNotificationModels();
        model.getNestedComponents();
        model.getSampleDataProviderModel();
        model.getSampleDataProviderModel().ifPresent(sampleDataProviderModel -> {
          sampleDataProviderModel.getParameters();
        });
        model.getErrorCallback().ifPresent(sourceCallbackModel -> {
          assertParameterizedModel(sourceCallbackModel);
          sourceCallbackModel.getModelProperties();
        });
        // All callbacks
      }

      @Override
      protected void onParameterGroup(ParameterizedModel owner, ParameterGroupModel model) {
        model.getModelProperties();
        model.getParameterModels();
        model.getExclusiveParametersModels();
      }

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        model.getAllowedStereotypes();
        model.getModelProperties();
        model.getFieldValueProviderModels();
        model.getSemanticTerms();
        model.getValueProviderModel().ifPresent(valueProviderModel -> valueProviderModel.getParameters());
      }

      @Override
      protected void onNestable(ComposableModel owner, NestableElementModel model) {
        model.getAllParameterModels();
        model.getModelProperties();
        model.getErrorModels();
        model.getParameterGroupModels();
        model.getNestedComponents();
        model.getSemanticTerms();
      }
    }.walk(result);
  }

  public void assertParameterizedModel(ParameterizedModel model) {
    model.getAllParameterModels();
    model.getParameterGroupModels();
  }

  public void assertNotNull(Object value) {
    assertThat(value, is(notNullValue()));
  }

}
