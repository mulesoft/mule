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
 * Defines a bootstrap property for a transaction factory
 */
public class TransactionFactoryBootstrapProperty extends AbstractBootstrapProperty {

  private final String transactionFactoryClassName;
  private final String transactionResourceClassName;

  /**
   * Creates a transaction bootstrap property
   *
   * @param service service that provides the property. Not null.
   * @param artifactType defines what is the artifact this bootstrap object applies to
   * @param optional indicates whether or not the bootstrap object is optional. When a bootstrap object is optional, any error
   *        creating it will be ignored.
   * @param transactionFactoryClassName key used to register the object. Not empty.
   * @param transactionResourceClassName className of the bootstrapped object. Not empty.
   */
  public TransactionFactoryBootstrapProperty(BootstrapService service, ArtifactType artifactType, Boolean optional,
                                             String transactionFactoryClassName, String transactionResourceClassName) {
    super(service, artifactType, optional);
    checkArgument(!StringUtils.isEmpty(transactionFactoryClassName), "key cannot be empty");
    checkArgument(!StringUtils.isEmpty(transactionResourceClassName), "className cannot be empty");

    this.transactionFactoryClassName = transactionFactoryClassName;
    this.transactionResourceClassName = transactionResourceClassName;
  }

  public String getTransactionFactoryClassName() {
    return transactionFactoryClassName;
  }

  public String getTransactionResourceClassName() {
    return transactionResourceClassName;
  }

  @Override
  public String toString() {
    return String.format("TransactionFactory{ %s}", transactionResourceClassName);
  }
}
