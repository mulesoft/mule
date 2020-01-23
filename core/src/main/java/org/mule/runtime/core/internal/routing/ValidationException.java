/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

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
