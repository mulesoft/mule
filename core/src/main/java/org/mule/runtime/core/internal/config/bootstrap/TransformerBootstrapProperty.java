/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
   * @param service         service that provides the property. Not null.
   * @param artifactTypes   defines what is the artifact this bootstrap object applies to
   * @param name            name assigned to the transformer. Can be null.
   * @param className       className of the bootstrapped transformer. Not empty.
   * @param returnClassName name of the transformer return class. Can be null.
   * @param mimeType        transformer returned mimeType. Can be null
   */
  public TransformerBootstrapProperty(BootstrapService service, Set<ArtifactType> artifactTypes, String name,
                                      String className, String returnClassName, String mimeType) {
    super(service, artifactTypes);
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
