/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
