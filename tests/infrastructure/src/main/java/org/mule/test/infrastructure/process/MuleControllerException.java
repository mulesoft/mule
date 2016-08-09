/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;


public class MuleControllerException extends RuntimeException {

  private static final long serialVersionUID = -235062000492669536L;

  /**
   * Constructs a new runtime exception with null as its detail message. The cause is not initialized, and may subsequently be
   * initialized by a call to {@link Throwable#initCause(Throwable)}.
   */
  public MuleControllerException() {
    super();
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may subsequently be
   * initialized by a call to {@link Throwable#initCause(Throwable)}.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()}
   *        method.
   */
  public MuleControllerException(String message) {
    super(message);
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of (cause==null ? null : cause.toString())
   * (which typically contains the class and detail message of cause). This constructor is useful for runtime exceptions that are
   * little more than wrappers for other throwables.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link Throwable#getCause()} method). (A null value is
   *        permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public MuleControllerException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   * </p>
   * Note that the detail message associated with cause is not automatically incorporated in this runtime exception's detail
   * message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link Throwable#getMessage()} method).
   * @param cause the cause (which is saved for later retrieval by the {@link Throwable#getCause()} method). (A null value is
   *        permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public MuleControllerException(String message, Throwable cause) {
    super(message, cause);
  }

}
