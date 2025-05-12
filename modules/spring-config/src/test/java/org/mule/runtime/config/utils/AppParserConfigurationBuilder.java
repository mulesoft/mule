/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.utils;

import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseAndBuildAppExtensionModel;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.module.artifact.activation.api.ast.AstXmlParserSupplier;

import java.util.Set;

public final class AppParserConfigurationBuilder extends AbstractConfigurationBuilder implements AstXmlParserSupplier {

  private final String[] configFiles;

  public AppParserConfigurationBuilder(String[] configFiles) {
    this.configFiles = configFiles;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    ArtifactAst artifactAst;
    if (configFiles.length == 0) {
      artifactAst = emptyArtifact();
    } else {
      artifactAst = parseAndBuildAppExtensionModel(configFiles, this, muleContext.getExtensionManager().getExtensions(),
                                                   APPLICATION, false, muleContext, null);
    }
    new ArtifactAstConfigurationBuilder(artifactAst, emptyMap(), APP, false, false)
        .configure(muleContext);
  }

  @Override
  public AstXmlParser getParser(Set<ExtensionModel> extensions, boolean disableValidations) {
    return AstXmlParser.builder()
        .withArtifactType(APPLICATION)
        .withExtensionModels(extensions)
        .build();
  }
}
