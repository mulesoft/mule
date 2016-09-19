/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

/**
 * Factory for creating instance of {@code ToolingArtifactBuilder}
 *
 * @since 4.0
 */
public interface TemporaryArtifactBuilderFactory {

  /**
   * @return a {@code ToolingArtifactBuilder} used to construct instance of {@link TemporaryArtifact}
   */
  TemporaryArtifactBuilder newBuilder();

}
