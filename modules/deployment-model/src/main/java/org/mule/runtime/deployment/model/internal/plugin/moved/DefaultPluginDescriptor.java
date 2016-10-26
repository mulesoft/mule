/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.deployment.model.api.plugin.moved.Descriptor;
import org.mule.runtime.deployment.model.api.plugin.moved.PluginDescriptor;

import java.util.Optional;

/**
 * Default implementation of {@link PluginDescriptor}
 *
 * @since 4.0
 */
public class DefaultPluginDescriptor implements PluginDescriptor {

  private final String name;
  private final MuleVersion minMuleVersion;
  private final Descriptor classloaderDescriptor;
  private final Optional<Descriptor> extensionModelDescriptor;

  public DefaultPluginDescriptor(String name, MuleVersion minMuleVersion, Descriptor classloaderDescriptor,
                                 Optional<Descriptor> extensionModelDescriptor) {
    this.name = name;
    this.minMuleVersion = minMuleVersion;
    this.classloaderDescriptor = classloaderDescriptor;
    this.extensionModelDescriptor = extensionModelDescriptor;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public MuleVersion getMinMuleVersion() {
    return minMuleVersion;
  }

  @Override
  public Descriptor getClassloaderModelDescriptor() {
    return classloaderDescriptor;
  }

  @Override
  public Optional<Descriptor> getExtensionModelDescriptor() {
    return extensionModelDescriptor;
  }
}
