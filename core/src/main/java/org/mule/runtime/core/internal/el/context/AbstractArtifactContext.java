/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.context;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;

import java.util.Map;

/**
 * Base class for exposing the artifact context to the expression language.
 *
 * Exposes information about the current Mule Application:
 * <li><b>encoding</b> <i>Artifact default encoding</i>
 * <li><b>name</b> <i>Artifact name</i>
 * <li><b>registry</b> <i>Artifact registry (as a map)</i>
 * <li><b>standalone</b> <i>If Mule is running standalone</i>
 * <li><b>workdir</b> <i>Artifact work directory</i>
 *
 * @since 4.0
 */
public abstract class AbstractArtifactContext {

  protected MuleContext muleContext;
  protected Registry registry;

  public AbstractArtifactContext(MuleContext muleContext, Registry registry) {
    this.muleContext = muleContext;
    this.registry = registry;
  }

  public String getName() {
    return muleContext.getConfiguration().getId();
  }

  public String getWorkDir() {
    return muleContext.getConfiguration().getWorkingDirectory();
  }

  public String getEncoding() {
    return muleContext.getConfiguration().getDefaultEncoding();
  }

  public boolean isStandalone() {
    return muleContext.getConfiguration().isStandalone();
  }

  public Map<String, Object> getRegistry() {
    return createRegistry(registry);
  }

  /**
   * Creates the registry wrapper to expose to the EL.
   *
   * @param registry the mule internal registry
   * @return the registry wrapper.
   */
  protected abstract Map<String, Object> createRegistry(Registry registry);

}
