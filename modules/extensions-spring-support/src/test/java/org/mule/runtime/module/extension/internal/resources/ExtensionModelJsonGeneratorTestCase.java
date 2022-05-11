/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase.ResourceExtensionUnitTest.newUnitTest;

import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.extension.test.extension.reconnection.ReconnectionExtension;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.data.sample.extension.SampleDataExtension;
import org.mule.test.function.extension.WeaveFunctionExtension;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.nonimplicit.config.extension.extension.api.NonImplicitConfigExtension;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.ram.RickAndMortyExtension;
import org.mule.test.semantic.extension.SemanticTermsExtension;
import org.mule.test.substitutiongroup.extension.SubstitutionGroupExtension;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.transactional.TransactionalExtension;
import org.mule.test.typed.value.extension.extension.TypedValueExtension;
import org.mule.test.values.extension.ValuesExtension;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Collection;
import java.util.List;

import org.skyscreamer.jsonassert.JSONAssert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class ExtensionModelJsonGeneratorTestCase extends FileGenerationParameterizedExtensionModelTestCase {

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionModelJson.updateExpectedFilesOnError");

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    List<ResourceExtensionUnitTest> extensions;
    extensions = asList(newUnitTest(JAVA_LOADER, VeganExtension.class, "vegan.json"),
                        newUnitTest(JAVA_LOADER, PetStoreConnector.class, "petstore.json"),
                        newUnitTest(JAVA_LOADER, MetadataExtension.class, "metadata.json"),
                        newUnitTest(JAVA_LOADER, HeisenbergExtension.class, "heisenberg.json"),
                        newUnitTest(JAVA_LOADER, SubstitutionGroupExtension.class, "substitutiongroup.json"),
                        newUnitTest(JAVA_LOADER, TransactionalExtension.class, "tx-ext.json"),
                        newUnitTest(JAVA_LOADER, SubTypesMappingConnector.class, "subtypes.json"),
                        newUnitTest(JAVA_LOADER, MarvelExtension.class, "marvel.json"),
                        newUnitTest(SOAP_LOADER, RickAndMortyExtension.class, "ram.json"),
                        newUnitTest(JAVA_LOADER, TypedValueExtension.class, "typed-value.json"),
                        newUnitTest(JAVA_LOADER, TestOAuthExtension.class, "test-oauth.json"),
                        newUnitTest(JAVA_LOADER, WeaveFunctionExtension.class, "test-fn.json"),
                        newUnitTest(JAVA_LOADER, ValuesExtension.class, "values.json"),
                        newUnitTest(JAVA_LOADER, SampleDataExtension.class, "sample-data.json"),
                        newUnitTest(JAVA_LOADER, ImplicitConfigExtension.class, "implicit-config.json"),
                        newUnitTest(JAVA_LOADER, NonImplicitConfigExtension.class, "non-implicit-config.json"),
                        newUnitTest(JAVA_LOADER, SemanticTermsExtension.class, "semantic-terms-extension.json"),
                        newUnitTest(JAVA_LOADER, ReconnectionExtension.class, "reconnection-extension.json"));

    return createExtensionModels(extensions);
  }

  private final ExtensionModelJsonSerializer generator = new ExtensionModelJsonSerializer(true);

  @Override
  protected boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

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
    assertThat(result, is(extensionUnderTest));
  }
}
