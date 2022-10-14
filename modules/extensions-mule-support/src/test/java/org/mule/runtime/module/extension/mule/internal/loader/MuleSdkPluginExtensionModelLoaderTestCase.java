/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.EXTENSION_EXTENSION_MODEL;

import static java.lang.Boolean.getBoolean;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.rules.ExpectedException.none;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.mule.internal.loader.ast.AbstractMuleSdkAstTestCase;
import org.mule.runtime.module.extension.mule.internal.operation.FakeExpressionLanguageMetadataService;
import org.mule.tck.classlaoder.TestClassLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;

@Feature(REUSE)
@Story(EXTENSION_EXTENSION_MODEL)
public class MuleSdkPluginExtensionModelLoaderTestCase extends AbstractMuleSdkAstTestCase {

  private static final Logger LOGGER = getLogger(MuleSdkPluginExtensionModelLoaderTestCase.class);
  private static final ExpressionLanguageMetadataService expressionLanguageMetadataService =
      new FakeExpressionLanguageMetadataService();
  private static final ArtifactCoordinates TEST_ARTIFACT_COORDINATES = new BundleDescriptor.Builder()
      .setArtifactId("TestExtension")
      .setGroupId("TestGroup")
      .setVersion("1.2.3")
      .build();

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionModelJson.updateExpectedFilesOnError");

  private final ExtensionModelJsonSerializer serializer = new ExtensionModelJsonSerializer();

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void whenResourceIsNotFoundThenFailsWithConfigurationException() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectCause(instanceOf(ConfigurationException.class));
    expectedException.expectMessage("extensions/non-existent.xml");
    getExtensionModelFrom("extensions/non-existent.xml");
  }

  @Test
  public void whenExtensionIsNotValidThenFailsDuringParsingValidations() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Attribute 'name' must appear on element 'extension'");
    getExtensionModelFrom("extensions/extension-without-name.xml");
  }

  @Test
  public void whenResourceIsAppInsteadOfExtensionThenFails() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException
        .expectMessage("Expected a single top level component matching identifier [extension:extension], but got: [flow]");
    getExtensionModelFrom("app-as-mule-extension.xml");
  }

  @Test
  public void whenResourceIsEmptyAppInsteadOfExtensionThenFails() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException
        .expectMessage("Expected a single top level component matching identifier [extension:extension]");
    getExtensionModelFrom("mule-empty-app-config.xml");
  }

  @Test
  public void loadingIsDoneWithTheSpecifiedClassLoader() {
    URL existentResource = getResourceAsUrl("extensions/extension-fully-parameterized.xml", this.getClass(), true, true);
    assertThat(existentResource, is(notNullValue()));

    // Creates a TestClassloader which has non-existent resource name mapped to an existent valid resource.
    final String nonexistentResourceName = "nonExistentResource";
    TestClassLoader testClassLoader = new TestClassLoader(this.getClass().getClassLoader());
    testClassLoader.addResource(nonexistentResourceName, existentResource);

    // Trying to load the model from the resource by its non-existent name should only succeed if the right class loader is used.
    getExtensionModelFrom(nonexistentResourceName, testClassLoader);

    // Control test to verify that loading from the non-existent name will actually fail if using the wrong class loader.
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectCause(hasCause(instanceOf(FileNotFoundException.class)));
    getExtensionModelFrom(nonexistentResourceName);
  }

  @Test
  public void loadExtensionExtensionModel() throws Exception {
    ExtensionModel extensionModel = getExtensionModelFrom("extensions/extension-fully-parameterized.xml");

    // Compares the obtained extension model against an expected serialization of it.
    String actualSerialized = serializer.serialize(extensionModel);
    String expectedSerializedPath = "models/extension-fully-parameterized.json";
    String expectedSerialized = getResourceAsString(expectedSerializedPath, getClass());
    try {
      JSONAssert.assertEquals(expectedSerialized, actualSerialized, true);
    } catch (AssertionError e) {
      if (shouldUpdateExpectedFilesOnError()) {
        updateExpectedJson(expectedSerializedPath, actualSerialized);
      } else {
        LOGGER.error(actualSerialized);
        throw e;
      }
    }

    // Asserts the correctness of the licensing information explicitly because non-public model properties are not serialized
    LicenseModelProperty expectedLicensingProperty = extensionModel.getModelProperty(LicenseModelProperty.class).get();
    assertThat(expectedLicensingProperty.requiresEeLicense(), is(true));
    assertThat(expectedLicensingProperty.isAllowsEvaluationLicense(), is(false));
    assertThat(expectedLicensingProperty.getRequiredEntitlement(), is(of("Premium Extension")));
  }

  @Test
  public void whenExtensionHasSelfReferencesThenTheExtensionModelIsCorrect() {
    ExtensionModel extensionModel = getExtensionModelFrom("extensions/extension-self-referencing.xml");
    assertThat(extensionModel.getOperationModels(), hasSize(2));
  }

  @Test
  public void whenExtensionModelIsLoadedThenArtifactCoordinatesAreCorrect() throws Exception {
    ExtensionModel extensionModel = getExtensionModelFrom("extensions/extension-fully-parameterized.xml");
    assertThat(extensionModel.getArtifactCoordinates().get(), is(TEST_ARTIFACT_COORDINATES));
  }

  private ExtensionModel getExtensionModelFrom(String extensionFile) {
    return getExtensionModelFrom(extensionFile, this.getClass().getClassLoader());
  }

  private ExtensionModel getExtensionModelFrom(String extensionFile, ClassLoader classLoader) {
    ExtensionModelLoadingRequest loadingRequest = builder(classLoader, getDefault(runtimeExtensionModels))
        .addParameter(VERSION_PROPERTY_NAME, TEST_ARTIFACT_COORDINATES.getVersion())
        .addParameter(MULE_SDK_RESOURCE_PROPERTY_NAME, extensionFile)
        .addParameter(MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME, expressionLanguageMetadataService)
        .setArtifactCoordinates(TEST_ARTIFACT_COORDINATES)
        .build();
    return new MuleSdkPluginExtensionModelLoader().loadExtensionModel(loadingRequest);
  }

  @Override
  protected String getConfigFile() {
    // This test should not be using this and use getExtensionModelFrom instead.
    return null;
  }

  /**
   * Utility to batch fix input files when severe model changes are introduced. Use carefully, not a mechanism to get away with
   * anything. First check why the generated json is different and make sure you're not introducing any bugs. This should NEVER be
   * committed as true
   *
   * @return whether the "expected" test files should be updated when comparison fails
   */
  private boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

  private void updateExpectedJson(String expectedJsonPath, String json) throws URISyntaxException, IOException {
    File root = new File(getResourceAsUrl(expectedJsonPath, getClass()).toURI()).getParentFile()
        .getParentFile().getParentFile().getParentFile();
    File testDir = new File(root, "src/test/resources/");
    File target = new File(testDir, expectedJsonPath);
    stringToFile(target.getAbsolutePath(), json);

    LOGGER.info(target.getAbsolutePath() + " was fixed");
  }
}
