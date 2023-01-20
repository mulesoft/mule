/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;

import java.io.File;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class HeavyweightApiDependenciesTestCase extends AbstractApiDependenciesTestCase {

  @Override
  protected String getDeploymentType() {
    return "heavyweight";
  }

  @Override
  protected DeployableProjectModelBuilder getDeployableProjectModelBuilder(File deployableFolder) {
    return new MuleDeployableProjectModelBuilder(deployableFolder);
  }
}
