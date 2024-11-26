/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseAndBuildAppExtensionModel;

import static java.util.Collections.emptyMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.module.artifact.activation.api.ast.AstXmlParserSupplier;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Set;

import org.junit.Test;

public class GenericServerNotificationManagerConfiguratorTestCase extends AbstractMuleContextTestCase {

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new AppParserConfigurationBuilder(new String[] {"./generic-server-notification-manager-test.xml"});
  }

  @Test
  public void testRegistryHasAGenericServerNotificationManagerIfNoDynamicConfigIsPresent() {
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_NOTIFICATION_MANAGER), notNullValue());
  }

  private static class AppParserConfigurationBuilder extends AbstractConfigurationBuilder implements AstXmlParserSupplier {

    private final String[] configFiles;

    private AppParserConfigurationBuilder(String[] configFiles) {
      this.configFiles = configFiles;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception {
      ArtifactAst artifactAst;
      if (configFiles.length == 0) {
        artifactAst = emptyArtifact();
      } else {
        artifactAst = parseAndBuildAppExtensionModel(muleContext.getConfiguration().getId(),
                                                     configFiles, this, muleContext.getExtensionManager().getExtensions(), false,
                                                     muleContext.getExecutionClassLoader(), muleContext.getConfiguration(), null);
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
}
