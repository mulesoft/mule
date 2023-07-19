/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.internal.artifact;

import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;

/**
 * Simple implementation of {@link ArtifactContext}
 *
 * @since 4.2
 */
public class ImmutableArtifactContext implements ArtifactContext {

  private final MuleContext muleContext;
  private final Registry registry;

  public ImmutableArtifactContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    registry = new DefaultRegistry(muleContext);
  }

  @Override
  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public Registry getRegistry() {
    return registry;
  }

  @Override
  public ArtifactAst getArtifactAst() {
    return emptyArtifact();
  }
}
