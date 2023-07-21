/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.exception.MuleException;

/**
 * Indicates that the execution of the current event is stopped. This exception is thrown to indicate this condition to the source
 * of the flow.
 * 
 * @since 4.0
 */
public class ValidationException extends MuleException {

  private static final long serialVersionUID = -4883468665512566232L;

}
