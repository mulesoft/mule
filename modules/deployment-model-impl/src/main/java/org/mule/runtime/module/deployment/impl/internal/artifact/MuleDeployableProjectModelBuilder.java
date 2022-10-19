/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;

import java.io.File;

/**
 * Implementation of {@link DeployableProjectModelBuilder} that builds a model based on the files provided within a packaged Mule
 * deployable artifact project.
 */
// TODO - W-11086334: add support for lightweight packaged projects
@Deprecated
public class MuleDeployableProjectModelBuilder
    extends org.mule.runtime.module.artifact.activation.internal.deployable.MuleDeployableProjectModelBuilder {

  public MuleDeployableProjectModelBuilder(File projectFolder) {
    super(projectFolder);
  }
}
