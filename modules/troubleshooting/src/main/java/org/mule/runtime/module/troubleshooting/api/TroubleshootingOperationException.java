/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

/**
 * Exception used to the troubleshooting operations related errors.
 *
 * @since 4.5
 */
@Experimental
public class TroubleshootingOperationException extends Exception {

  private static final long serialVersionUID = -6535704838521318202L;

  public TroubleshootingOperationException(String message, Exception cause) {
    super(message, cause);
  }

  public TroubleshootingOperationException(String mesage) {
    this(mesage, null);
  }
}
