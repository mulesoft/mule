/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import org.mule.runtime.extension.api.annotation.Alias;

public class AbstractTestDocumentedOperations {

  /**
   * Inherited Operation Documentation
   *
   * @param value parameter documentation for an inherited operation.
   */
  public void inheritedOperation(String value) {

  }

  /**
   * Ope with Alias
   *
   * @param value some doc
   */
  @Alias("operation-with-alias")
  public void aliased(String value) {

  }
}
