/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.test.infrastructure.maven.MavenTestUtils.getMavenLocalRepository;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;

public abstract class DeclarationSessionTestCase extends AbstractFakeMuleServerTestCase implements TestExtensionAware {

  protected static final String EXTENSION_GROUP_ID = "org.mule.tooling";
  protected static final String EXTENSION_ARTIFACT_ID = "tooling-support-test-extension";
  protected static final String EXTENSION_VERSION = "1.0.0-SNAPSHOT";
  protected static final String EXTENSION_CLASSIFIER = "mule-plugin";
  protected static final String EXTENSION_TYPE = "jar";

  protected static final String CONFIG_NAME = "dummyConfig";
  protected static final String CLIENT_NAME = "client";
  protected static final String PROVIDED_PARAMETER_NAME = "providedParameter";
  protected static final String WITH_ACTING_PARAMETER = "WITH-ACTING-PARAMETER-";

  protected DeclarationSession session;

  @ClassRule
  public static SystemProperty artifactsLocation =
      new SystemProperty("mule.test.maven.artifacts.dir", DeclarationSession.class.getResource("/").getPath());

  @Rule
  public SystemProperty repositoryLocation =
      new SystemProperty("muleRuntimeConfig.maven.repositoryLocation", getMavenLocalRepository().getAbsolutePath());

  @Override
  public void setUp() throws Exception {
    ArtifactDeclarer artifactDeclarer = newArtifact();
    declareArtifact(artifactDeclarer);
    super.setUp();
    this.session = this.muleServer
        .toolingService()
        .newDeclarationSessionBuilder()
        .addDependency(EXTENSION_GROUP_ID,
                       EXTENSION_ARTIFACT_ID,
                       EXTENSION_VERSION,
                       EXTENSION_CLASSIFIER,
                       EXTENSION_TYPE)
        .setArtifactDeclaration(artifactDeclarer.getDeclaration())
        .build();
    this.muleServer.start();
  }

  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_NAME, connectionDeclaration(CLIENT_NAME)));
  }

  @After
  public void disposeSession() {
    if (session != null) {
      session.dispose();
    }
  }

}
