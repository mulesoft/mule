/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;

import static org.mule.extension.ws.api.exception.WscErrors.BAD_REQUEST;
import static org.mule.extension.ws.api.exception.WscErrors.BAD_RESPONSE;
import static org.mule.extension.ws.api.exception.WscErrors.ENCODING;
import static org.mule.extension.ws.api.exception.WscErrors.INVALID_WSDL;
import static org.mule.extension.ws.api.exception.WscErrors.SOAP_FAULT;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * {@link ErrorTypeProvider} implementation for the {@link ConsumeOperation}.
 *
 * @since 4.0
 */
public class ConsumeErrorTypeProvider implements ErrorTypeProvider {

  /**
   * @return all the error types that can be thrown by the {@link ConsumeOperation}.
   */
  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder().add(BAD_REQUEST, BAD_RESPONSE, ENCODING, INVALID_WSDL, SOAP_FAULT).build();
  }
}
