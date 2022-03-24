/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.builders;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.internal.config.bootstrap.SimpleRegistryBootstrap;

public interface RegistryBootstrap extends Initialisable {

  static RegistryBootstrap defaultRegistryBoostrap(ArtifactType supportedArtifactType, MuleContext muleContext) {
    return new SimpleRegistryBootstrap(supportedArtifactType, muleContext);
  }

  @Override
  void initialise() throws InitialisationException;
}
