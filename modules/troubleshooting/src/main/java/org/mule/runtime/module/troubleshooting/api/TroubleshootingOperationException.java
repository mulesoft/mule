package org.mule.runtime.module.troubleshooting.api;

/**
 * Exception used to the troubleshooting operations related errors.
 */
public class TroubleshootingOperationException extends Exception {

  private static final long serialVersionUID = -6535704838521318202L;

  public TroubleshootingOperationException(String message, Exception cause) {
    super(message, cause);
  }

  public TroubleshootingOperationException(String mesage) {
    this(mesage, null);
  }
}
