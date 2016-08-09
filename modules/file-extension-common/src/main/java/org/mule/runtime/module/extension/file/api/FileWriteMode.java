/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

/**
 * List different strategies regarding how to write new files
 *
 * @since 4.0
 */
public enum FileWriteMode {
  /**
   * Means that if the file to be written already exists, then it should be overwritten
   */
  OVERWRITE,

  /**
   * Means that if the file to be written already exists, then the content should be appended to that file
   */
  APPEND,

  /**
   * Means that a new file should be created and an error should be raised if the file already exists
   */
  CREATE_NEW
}
