/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.api.annotation.NoImplement;

import java.io.Serializable;

/**
 * <code>Statistics</code> TODO
 * 
 */
@NoImplement
public interface Statistics extends Serializable {

  /**
   * Are statistics logged
   */
  boolean isEnabled();

}
