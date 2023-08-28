/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.artifact.api.Artifact;

/**
 * An artifact context contains all the information related to an {@link Artifact} that contains configuration.
 *
 * @since 4.0
 */
@NoImplement
public interface ArtifactContext {

  /**
   * @return the artifact {@link MuleContext}
   */
  MuleContext getMuleContext();

  /**
   * @return the registry of the artifact.
   */
  Registry getRegistry();

  /**
   * @return the ast representation of this artifact.
   */
  ArtifactAst getArtifactAst();

}
