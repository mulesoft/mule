/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.descriptor;

import java.io.File;
import java.io.IOException;

/**
 * Parses an artifact descriptor
 */
public interface DescriptorParser<D extends DeployableArtifactDescriptor> {

  /**
   * Parses an artifact descriptor and creates a {@link DeployableArtifactDescriptor} with
   * the information from the descriptor.
   *
   * @param artifactLocation the location of the artifact. This is the folder where the artifact content is stored.
   * @param descriptor file that contains the descriptor content
   * @param artifactName name of the artifact
   * @return a descriptor with all the information of the descriptor file.
   * @throws IOException
     */
  D parse(File artifactLocation, File descriptor, String artifactName) throws IOException;
}
