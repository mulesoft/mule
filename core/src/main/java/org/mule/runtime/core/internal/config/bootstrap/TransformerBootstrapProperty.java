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
 * Defines a bootstrap property for a transformer
 */
public class TransformerBootstrapProperty extends AbstractBootstrapProperty {

  private final String name;
  private final String className;
  private final String mimeType;
  private final String returnClassName;

  /**
   * Creates a bootstrap property
   *
   * @param service service that provides the property. Not null.
   * @param artifactType defines what is the artifact this bootstrap object applies to
   * @param optional indicates whether or not the bootstrapped transformer is optional. When a bootstrap object is optional, any
   *        error creating it will be ignored.
   * @param name name assigned to the transformer. Can be null.
   * @param className className of the bootstrapped transformer. Not empty.
   * @param returnClassName name of the transformer return class. Can be null.
   * @param mimeType transformer returned mimeType. Can be null
   */
  public TransformerBootstrapProperty(BootstrapService service, ArtifactType artifactType, boolean optional, String name,
                                      String className, String returnClassName, String mimeType) {
    super(service, artifactType, optional);
    checkArgument(!StringUtils.isEmpty(className), "className cannot be empty");

    this.name = name;
    this.className = className;
    this.mimeType = mimeType;
    this.returnClassName = returnClassName;
  }

  public String getName() {
    return name;
  }

  public String getClassName() {
    return className;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getReturnClassName() {
    return returnClassName;
  }

  @Override
  public String toString() {
    return String.format("Transformer{ %s}", className);
  }
}
