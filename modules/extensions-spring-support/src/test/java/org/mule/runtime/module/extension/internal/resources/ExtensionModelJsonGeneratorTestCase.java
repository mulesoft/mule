/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.extension.test.extension.reconnection.ReconnectionExtension;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tck.size.SmallTest;
import org.mule.test.data.sample.extension.SampleDataExtension;
import org.mule.test.function.extension.WeaveFunctionExtension;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.test.nonimplicit.config.extension.extension.api.NonImplicitConfigExtension;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.semantic.extension.SemanticTermsExtension;
import org.mule.test.substitutiongroup.extension.SubstitutionGroupExtension;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.transactional.TransactionalExtension;
import org.mule.test.typed.value.extension.extension.TypedValueExtension;
import org.mule.test.values.extension.ValuesExtension;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

@SmallTest
@RunWith(Parameterized.class)
public class ExtensionModelJsonGeneratorTestCase extends FileGenerationParameterizedExtensionModelTestCase {

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionModelJson.updateExpectedFilesOnError");

  private static final String TEST_GROUP_ID = "org.mule.tests";
  private static final String VERSION = "1.2.3";

  @Parameterized.Parameters(name = "{2}")
  public static Collection<Object[]> data() {
    return asList(
                  new Object[] {JAVA_LOADER, VeganExtension.class, "vegan.json",
                      createArtifactCoordinate("mule-vegan-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, PetStoreConnector.class, "petstore.json",
                      createArtifactCoordinate("mule-petstore-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, MetadataExtension.class, "metadata.json",
                      createArtifactCoordinate("mule-metadata-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, HeisenbergExtension.class, "heisenberg.json",
                      createArtifactCoordinate("mule-heisenberg-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, SubstitutionGroupExtension.class, "substitutiongroup.json",
                      createArtifactCoordinate("mule-substitution-group-extension"),
                      singletonList(HeisenbergExtension.class)},
                  new Object[] {JAVA_LOADER, TransactionalExtension.class, "tx-ext.json",
                      createArtifactCoordinate("mule-tx-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, SubTypesMappingConnector.class, "subtypes.json",
                      createArtifactCoordinate("mule-subtypes-extension"),
                      asList(HeisenbergExtension.class, MetadataExtension.class, VeganExtension.class)},
                  new Object[] {JAVA_LOADER, MarvelExtension.class, "marvel.json",
                      createArtifactCoordinate("mule-marvel-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, TypedValueExtension.class, "typed-value.json",
                      createArtifactCoordinate("mule-typed-value-extension"),
                      singletonList(HeisenbergExtension.class)},
                  new Object[] {JAVA_LOADER, TestOAuthExtension.class, "test-oauth.json",
                      createArtifactCoordinate("mule-test-oauth-extension"),
                      singletonList(ValuesExtension.class)},
                  new Object[] {JAVA_LOADER, WeaveFunctionExtension.class, "test-fn.json",
                      createArtifactCoordinate("test-weave-function-extension"),
                      singletonList(HeisenbergExtension.class)},
                  new Object[] {JAVA_LOADER, ValuesExtension.class, "values.json",
                      createArtifactCoordinate("mule-values-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, SampleDataExtension.class, "sample-data.json",
                      createArtifactCoordinate("mule-sample-data-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, ImplicitConfigExtension.class, "implicit-config.json",
                      createArtifactCoordinate("mule-implicit-config-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, NonImplicitConfigExtension.class, "non-implicit-config.json",
                      createArtifactCoordinate("mule-non-implicit-config-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, SemanticTermsExtension.class, "semantic-terms-extension.json",
                      createArtifactCoordinate("mule-semantic-terms-extension"),
                      emptyList()},
                  new Object[] {JAVA_LOADER, ReconnectionExtension.class, "reconnection-extension.json",
                      createArtifactCoordinate("mule-reconnection-test-extension"),
                      emptyList()});
  }

  @Parameterized.Parameter(1)
  public Class extensionClass;

  protected static ArtifactCoordinates createArtifactCoordinate(String artifactId) {
    return new BundleDescriptor.Builder().setGroupId(TEST_GROUP_ID).setArtifactId(artifactId).setVersion(VERSION).build();
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
  protected String doGenerate(ExtensionModel extensionUnderTest) {
    return generator.serialize(extensionUnderTest).trim();
  }

  @Override
  protected void assertEquals(String expectedContent, String actualContent) throws Exception {
    JSONAssert.assertEquals(expectedContent, actualContent, true);
  }

  @Test
  public void load() {
    ExtensionModel result = generator.deserialize(expectedContent);
    assertThat(result, is(doLoadExtension()));
  }

  @Override
  protected ExtensionModel doLoadExtension() {
    return loadExtension(extensionClass, loader, artifactCoordinates, dslResolvingContext);
  }
}
