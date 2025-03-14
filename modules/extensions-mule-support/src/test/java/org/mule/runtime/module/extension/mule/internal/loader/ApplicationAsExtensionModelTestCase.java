/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.APPLICATION_EXTENSION_MODEL;

import static java.lang.Boolean.getBoolean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.function.HasFunctionModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import jakarta.inject.Inject;

import org.skyscreamer.jsonassert.JSONAssert;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(APPLICATION_EXTENSION_MODEL)
public class ApplicationAsExtensionModelTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  private final ExtensionModelJsonSerializer serializer = new ExtensionModelJsonSerializer(true);

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionModelJson.updateExpectedFilesOnError");

  @Override
  protected String getConfigFile() {
    return "app-as-mule-extension.xml";
  }

  @Test
  public void loadApplicationExtensionModel() throws Exception {
    ExtensionModel extensionModel = getAppExtensionModel();
    String actual = serializer.serialize(extensionModel);
    final String expectedSerializedPath = "models/app-as-mule-extension.json";
    String expected = getResourceAsString(expectedSerializedPath, getClass());
    try {
      JSONAssert.assertEquals(expected, actual, true);
    } catch (AssertionError e) {
      if (shouldUpdateExpectedFilesOnError()) {
        updateExpectedJson(expectedSerializedPath, actual);
      } else {
        LOGGER.error(actual);
        throw e;
      }
    }
  }

  @Test
  public void flowIsIgnored() {
    ExtensionModel extensionModel = getAppExtensionModel();

    new ExtensionWalker() {

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        assertNotFlow(model);
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        assertNotFlow(model);
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        assertNotFlow(model);
      }

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        assertNotFlow(model);
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        assertNotFlow(model);
      }

      @Override
      protected void onFunction(HasFunctionModels owner, FunctionModel model) {
        assertNotFlow(model);
      }
    }.walk(extensionModel);
  }

  private void assertNotFlow(NamedObject named) {
    assertThat(named.getName(), is(not(equalTo("superfluousFlow"))));
  }

  private ExtensionModel getAppExtensionModel() {
    return extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
  }

  private String getResource(String path) {
    InputStream in = getClass().getResourceAsStream(path);
    checkArgument(in != null, "Resource not found: " + path);

    return IOUtils.toString(in);
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
