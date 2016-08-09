/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.sftp;

/**
 * Supported types of SFTP authentication methods
 *
 * @since 4.0
 */
public enum SftpAuthenticationMethod {
  GSSAPI_WITH_MIC("gssapi_with_mic"), PUBLIC_KEY("publickey"), KEYBOARD_INTERACTIVE("keyboard_interactive"), PASSWORD("password");

  SftpAuthenticationMethod(String code) {
    this.code = code;
  }

  private final String code;

  /**
   * @return the method's protocol name
   */
  @Override
  public String toString() {
    return code;
  }
}
