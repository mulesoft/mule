/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
