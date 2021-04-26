/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.schema;

import static com.google.gson.JsonParser.parseString;
import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase.ResourceExtensionUnitTest.newUnitTest;

import org.mule.extension.test.extension.reconnection.ReconnectionExtension;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.connectivity.api.platform.schema.ConnectivitySchema;
import org.mule.runtime.connectivity.api.platform.schema.ExchangeAssetDescriptor;
import org.mule.runtime.connectivity.api.platform.schema.generator.ConnectivitySchemaGenerator;
import org.mule.runtime.connectivity.api.platform.schema.generator.ConnectivitySchemaGeneratorBuilder;
import org.mule.runtime.connectivity.api.platform.schema.persistence.ConnectivitySchemaJsonSerializer;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.data.sample.extension.SampleDataExtension;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.nonimplicit.config.extension.extension.api.NonImplicitConfigExtension;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.ram.RickAndMortyExtension;
import org.mule.test.semantic.extension.SemanticTermsExtension;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.transactional.TransactionalExtension;
import org.mule.test.values.extension.ValuesExtension;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

@SmallTest
@RunWith(Parameterized.class)
public class ConnectivitySchemaJsonSerializerTestCase extends FileGenerationParameterizedExtensionModelTestCase {

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
          getBoolean(SYSTEM_PROPERTY_PREFIX + "connectivitySchemas.updateExpectedFilesOnError");


  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    final List<ResourceExtensionUnitTest> extensions;
    extensions = asList(newUnitTest(JAVA_LOADER, VeganExtension.class, "vegan.json"),
            newUnitTest(JAVA_LOADER, PetStoreConnector.class, "petstore.json"),
            newUnitTest(JAVA_LOADER, MetadataExtension.class, "metadata.json"),
            newUnitTest(JAVA_LOADER, HeisenbergExtension.class, "heisenberg.json"),
            newUnitTest(JAVA_LOADER, TransactionalExtension.class, "tx-ext.json"),
            newUnitTest(JAVA_LOADER, SubTypesMappingConnector.class, "subtypes.json"),
            newUnitTest(JAVA_LOADER, MarvelExtension.class, "marvel.json"),
            newUnitTest(SOAP_LOADER, RickAndMortyExtension.class, "ram.json"),
            newUnitTest(JAVA_LOADER, TestOAuthExtension.class, "test-oauth.json"),
            newUnitTest(JAVA_LOADER, ValuesExtension.class, "values.json"),
            newUnitTest(JAVA_LOADER, SampleDataExtension.class, "sample-data.json"),
            newUnitTest(JAVA_LOADER, ImplicitConfigExtension.class, "implicit-config.json"),
            newUnitTest(JAVA_LOADER, NonImplicitConfigExtension.class, "non-implicit-config.json"),
            newUnitTest(JAVA_LOADER, SemanticTermsExtension.class, "semantic-terms-extension.json"),
            newUnitTest(JAVA_LOADER, ReconnectionExtension.class, "reconnection-extension.json"));

    return createExtensionModels(extensions);
  }

  @Inject
  private ExpressionLanguage expressionLanguage;

  private final ConnectivitySchemaGenerator generator = ConnectivitySchemaGeneratorBuilder.newInstance()
          .setConnectionTermsExtractor(ConnectionProviderModel::getSemanticTerms)
          .setParameterTermsExtractor(ParameterModel::getSemanticTerms)
          .setTypeTermsExtractor(ExtensionMetadataTypeUtils::getSemanticTerms)
          .setConnectorAssetDescriptor(new ExchangeAssetDescriptor("org.mule.runtime.test.extension", "mule-connectivity-schema-test", "1.0.0"))
          .build();

  private final ConnectivitySchemaJsonSerializer serializer = ConnectivitySchemaJsonSerializer.builder().build();

  @Override
  protected String getExpectedFilesDir() {
    return "connectivity-schemas/";
  }

  @Override
  protected boolean shouldUpdateExpectedFilesOnError() {
//    return UPDATE_EXPECTED_FILES_ON_ERROR;
    return true;
  }

  @Override
  protected String doGenerate(ExtensionModel extensionUnderTest) throws Exception {
    JSONArray array = new JSONArray();
    List<ConnectivitySchema> schemas = generator.generateSchemas(extensionUnderTest);
    for (ConnectivitySchema schema : schemas) {
      array.put(new JSONObject(serializer.serialize(schema)));
    }

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(parseString(array.toString()));
  }

  @Override
  protected void assertEquals(String expectedContent, String actualContent) throws Exception{
    JSONAssert.assertEquals(expectedContent, actualContent, true);
  }
}
