/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

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
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.InputStream;

import javax.inject.Inject;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model"})
public class ApplicationAsExtensionModelTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  private ExtensionModelJsonSerializer serializer = new ExtensionModelJsonSerializer();

  @Override
  protected String getConfigFile() {
    return "app-as-mule-extension.xml";
  }

  @Test
  public void loadApplicationExtensionModel() throws Exception {
    ExtensionModel extensionModel = getAppExtensionModel();
    String json = serializer.serialize(extensionModel);
    String expected = getResource("/models/app-as-mule-extension.json");
    try {
      JSONAssert.assertEquals(expected, json, true);
    } catch (AssertionError e) {
      System.out.println(json);
      throw e;
    }
  }

  @Test
  public void salutationFlow() throws Exception {
    flowRunner("salutationFlow").run();
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
}
