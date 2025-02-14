/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.SERIALIZED_ARTIFACT_AST_LOCATION;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;

import io.qameta.allure.Issue;
import org.junit.Test;

public class SerializedAstDeploymentTestCase extends AbstractDeploymentTestCase {

  protected final ApplicationFileBuilder artifactFileBuilder =
      new ApplicationFileBuilder("serialized-with-new-error-type-in-error-handler")
          .definedBy("serialized-with-new-error-type-in-error-handler.xml")
          .containingResource("properties.yaml", "properties.yaml")
          .containingResource("serialized/serialized-with-new-error-type-in-error-handler.ast", SERIALIZED_ARTIFACT_AST_LOCATION);

  public SerializedAstDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  @Issue("W-17339304")
  public void whenErrorTypeInRaiseErrorUnresolvedPropertyAndSerializedASTDeploymentSuccessful() throws Exception {
    addPackedAppFromBuilder(artifactFileBuilder, null);
    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());
  }
}
