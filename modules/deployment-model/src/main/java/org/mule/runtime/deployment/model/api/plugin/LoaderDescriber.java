/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.meta.model.ExtensionModel;

/**
 * Generic descriptor that will be used to describe parameterization to construct {@link ExtensionModel}, {@link ClassLoader} and
 * any other descriptor that may arise in a future of {@link ArtifactPluginDescriptor}.
 * <p/>
 * Each {@link LoaderDescriber} will have an ID that will be used to discover any loader that's responsible of working with the
 * current set of attributes. It's up to each loader to validate the types, size and all that matters around the attributes.
 *
 * @since 4.0
 * @deprecated since 4.5 use org.mule.runtime.module.artifact.api.plugin.LoaderDescriber instead.
 */
@NoInstantiate
@Deprecated
public final class LoaderDescriber extends org.mule.runtime.module.artifact.api.plugin.LoaderDescriber {

  /**
   * Creates an immutable implementation of {@link LoaderDescriber}
   *
   * @param id ID of the descriptor. Not blank nor null.
   */
  public LoaderDescriber(String id) {
    super(id);
  }

}
