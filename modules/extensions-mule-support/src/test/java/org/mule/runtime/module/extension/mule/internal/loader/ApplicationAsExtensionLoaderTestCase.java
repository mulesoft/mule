/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.getLoaderById;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_TYPE_LOADER_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.type.ApplicationTypeLoader;

import java.io.InputStream;

import javax.inject.Inject;

import org.junit.Test;

public class ApplicationAsExtensionLoaderTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  @Override
  protected String getConfigFile() {
    return "app-as-mule-extension.xml";
  }

  @Test
  public void loadApplicationExtensionModel() throws Exception {
    ExtensionModel model = loadExtensionModel("TestApplication", "/app-as-mule-extension.xml");
    assertThat(model.getOperationModels(), hasSize(1));
  }

  private ExtensionModel loadExtensionModel(String artifactName, String path) {
    InputStream in = getClass().getResourceAsStream(path);
    checkState(in != null, "Could not find file " + path);

    AstXmlParser parser = AstXmlParser.builder()
        .withExtensionModels(extensionManager.getExtensions())
        .withArtifactType(APPLICATION)
        .build();

    ArtifactAst ast = parser.parse(artifactName, in);

    return getLoaderById(MULE_SDK_LOADER_ID)
        .loadExtensionModel(builder(getExecutionClassLoader(), getDefault(extensionManager.getExtensions()))
            .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, artifactName)
            .addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, ast)
            .addParameter(MULE_SDK_TYPE_LOADER_PROPERTY_NAME, new ApplicationTypeLoader())
            .build());
  }

}
