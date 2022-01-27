/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
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
    ExtensionModel extensionModel = extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
    String json = serializer.serialize(extensionModel);
    String expected = getResource("/models/app-as-mule-extension.json");
    JSONAssert.assertEquals(expected, json, true);
  }

  private String getResource(String path) {
    InputStream in = getClass().getResourceAsStream(path);
    checkArgument(in != null, "Resource not found: " + path);

    return IOUtils.toString(in);
  }
}
