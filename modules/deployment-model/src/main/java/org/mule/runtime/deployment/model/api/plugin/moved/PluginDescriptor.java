/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.moved;

import org.mule.runtime.api.meta.MuleVersion;

import java.util.Optional;

/**
 * Represents a mule plugin descriptor
 *
 * @since 4.0
 */
public interface PluginDescriptor {

  /**
   * @return plugin's name. Non null.
   */
  String getName();

  /**
   * @return the minimal mule version in which this plugin could run successfully. Non null.
   */
  MuleVersion getMinMuleVersion();

  /**
   * @return the object that holds the mandatory information to create, later on, an {@link org.mule.runtime.api.meta.model.ExtensionModel}. Non null.
   */
  Descriptor getClassloaderModelDescriptor();

  /**
   * @return the object that holds the mandatory information to create, later on, an {@link org.mule.runtime.api.meta.model.ExtensionModel}. Non null.
   */
  Optional<Descriptor> getExtensionModelDescriptor();
}
