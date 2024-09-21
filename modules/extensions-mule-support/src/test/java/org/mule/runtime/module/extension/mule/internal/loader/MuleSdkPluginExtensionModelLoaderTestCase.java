/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.TEST_ARTIFACT_COORDINATES;
import static org.mule.runtime.module.extension.mule.internal.loader.ExtensionModelTestUtils.loadMuleSdkExtension;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.EXTENSION_EXTENSION_MODEL;

import static java.lang.Boolean.getBoolean;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThrows;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.mule.internal.loader.ast.AbstractMuleSdkAstTestCase;
import org.mule.tck.classlaoder.TestClassLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(EXTENSION_EXTENSION_MODEL)
public class MuleSdkPluginExtensionModelLoaderTestCase extends AbstractMuleSdkAstTestCase {

  private static final Logger LOGGER = getLogger(MuleSdkPluginExtensionModelLoaderTestCase.class);

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionModelJson.updateExpectedFilesOnError");

  private final ExtensionModelJsonSerializer serializer = new ExtensionModelJsonSerializer(true);

  @Test
  public void whenResourceIsNotFoundThenFailsWithConfigurationException() {
    final var thrown = assertThrows(MuleRuntimeException.class, () -> getExtensionModelFrom("extensions/non-existent.xml"));

    assertThat(thrown.getCause(), instanceOf(ConfigurationException.class));
    assertThat(thrown.getMessage(), containsString("extensions/non-existent.xml"));
  }

  @Test
  public void whenExtensionWithoutDescriptionThenFailsDuringParsingValidations() {
    final var thrown =
        assertThrows(RuntimeException.class, () -> getExtensionModelFrom("extensions/extension-without-description.xml"));

    assertThat(thrown.getMessage(), containsString("The content of element 'extension' is not complete."));
  }

  @Test
  public void whenExtensionDoesNotHaveNameThenFailsDuringParsingValidations() {
    final var thrown = assertThrows(RuntimeException.class, () -> getExtensionModelFrom("extensions/extension-without-name.xml"));

    assertThat(thrown.getMessage(), containsString("Attribute 'name' must appear on element 'description'"));
  }

  @Test
  public void whenResourceIsAppInsteadOfExtensionThenFails() {
    final var thrown = assertThrows(MuleRuntimeException.class, () -> getExtensionModelFrom("app-as-mule-extension.xml"));

    assertThat(thrown.getMessage(),
               containsString("Extension from artifact 'TestExtension' is missing a required top level element. 'extension:description' is expected."));
  }

  @Test
  public void whenResourceIsEmptyAppInsteadOfExtensionThenFails() {
    final var thrown = assertThrows(MuleRuntimeException.class, () -> getExtensionModelFrom("mule-empty-app-config.xml"));

    assertThat(thrown.getMessage(),
               containsString("Extension from artifact 'TestExtension' is missing a required top level element. 'extension:description' is expected."));
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
    loadMuleSdkExtension(nonexistentResourceName, testClassLoader, astParserExtensionModels);

    // Control test to verify that loading from the non-existent name will actually fail if using the wrong class loader.
    final var thrown = assertThrows(MuleRuntimeException.class, () -> getExtensionModelFrom(nonexistentResourceName));
    assertThat(thrown.getCause().getCause(), instanceOf(FileNotFoundException.class));
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
    return loadMuleSdkExtension(extensionFile, this.getClass().getClassLoader(), astParserExtensionModels);
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
