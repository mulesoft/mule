/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.descriptor;

import org.mule.runtime.deployment.model.api.plugin.descriptor.Descriptor;

import java.util.Map;

/**
 * Default implementation class
 *
 * @since 4.0
 */
public class DefaultDescriptor implements Descriptor {

  private final String id;
  private final Map<String, Object> attributes;

  public DefaultDescriptor(String id, Map<String, Object> attributes) {
    this.id = id;
    this.attributes = attributes;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
