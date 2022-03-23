package org.mule.runtime.module.artifact.activation.api.config.boot;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.activation.internal.config.boot.SimpleRegistryBootstrap;

public interface RegistryBootstrap extends Initialisable {

  static RegistryBootstrap defaultRegistryBoostrap(ArtifactType supportedArtifactType, MuleContext muleContext) {
    return new SimpleRegistryBootstrap(supportedArtifactType, muleContext);
  }

  @Override
  void initialise() throws InitialisationException;
}
