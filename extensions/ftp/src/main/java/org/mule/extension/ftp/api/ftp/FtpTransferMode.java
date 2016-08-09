/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.ftp;

import org.apache.commons.net.ftp.FTP;

/**
 * Lists the supported transfer modes for FTP operations.
 */
public enum FtpTransferMode {
  /**
   * Represents the binary transfer mode
   */
  BINARY(FTP.BINARY_FILE_TYPE, "Binary"),

  /**
   * Represents the text transfer mode
   */
  ASCII(FTP.ASCII_FILE_TYPE, "Ascii");

  private final int code;
  private final String description;

  FtpTransferMode(int code, String description) {
    this.code = code;
    this.description = description;
  }

  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }
}
