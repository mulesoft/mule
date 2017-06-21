/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;

/**
 * Defines a bootstrap property for a generic object
 */
public class ObjectBootstrapProperty extends AbstractBootstrapProperty {

  private final String key;
  private final String className;

  /**
   * Creates a generic bootstrap property
   *
   * @param service service that provides the property. Not null.
   * @param artifactType defines what is the artifact this bootstrap object applies to
   * @param optional indicates whether or not the bootstrap object is optional. When a bootstrap object is optional, any error
   *        creating it will be ignored.
   * @param key key used to register the object. Not empty.
   * @param className className of the bootstrapped object. Not empty.
   */
  public ObjectBootstrapProperty(BootstrapService service, ArtifactType artifactType, Boolean optional, String key,
                                 String className) {
    super(service, artifactType, optional);
    checkArgument(!StringUtils.isEmpty(key), "key cannot be empty");
    checkArgument(!StringUtils.isEmpty(className), "className cannot be empty");

    this.key = key;
    this.className = className;
  }

  public String getKey() {
    return key;
  }

  public String getClassName() {
    return className;
  }

  @Override
  public String toString() {
    return String.format("Object{ %s}", className);
  }
}
