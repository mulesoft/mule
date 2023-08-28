/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source.poll;

/**
 * Enumeration that represent the possible outcomes of testing an item's watermark.
 *
 * @since 4.4.0
 */
public enum WatermarkStatus {

  /**
   * The item passed the watermark.
   */
  PASSED,

  /**
   * The item was rejected because it did not passed the watermark, of was already processed with that watermark value.
   */
  REJECT,

  /**
   * The item passed the watermark, and its watermark value is equal to the highest watermark value found.
   */
  ON_HIGH,

  /**
   * The items passed the watermark, and its watermark value is higher than any item tested before.
   */
  ON_NEW_HIGH
}
