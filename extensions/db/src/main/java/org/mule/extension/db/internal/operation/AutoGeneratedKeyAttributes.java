/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.List;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;

/**
 * Parameters to configure if and how should
 * keys be auto generated
 *
 * @since 4.0
 */
public class AutoGeneratedKeyAttributes {

  public static final String AUTO_GENERATED_KEYS = "Auto Generated Keys";
  /**
   * Indicates when to make auto-generated keys available for retrieval.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Placement(tab = ADVANCED, group = AUTO_GENERATED_KEYS, order = 1)
  private boolean autoGeneratedKeys = false;

  /**
   * List of column indexes that indicates which auto-generated keys to make
   * available for retrieval.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED, group = AUTO_GENERATED_KEYS, order = 2)
  private List<Integer> autoGeneratedKeysColumnIndexes;

  /**
   * List of column names that indicates which auto-generated keys should be made
   * available for retrieval.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED, group = AUTO_GENERATED_KEYS, order = 3)
  private List<String> autoGeneratedKeysColumnNames;

  public boolean isAutoGeneratedKeys() {
    return autoGeneratedKeys;
  }

  public List<Integer> getAutoGeneratedKeysColumnIndexes() {
    return autoGeneratedKeysColumnIndexes;
  }

  public List<String> getAutoGeneratedKeysColumnNames() {
    return autoGeneratedKeysColumnNames;
  }
}
