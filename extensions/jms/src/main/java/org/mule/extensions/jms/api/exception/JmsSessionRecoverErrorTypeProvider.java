/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import static org.mule.extensions.jms.api.exception.JmsErrors.SESSION_RECOVER;
import com.google.common.collect.ImmutableSet;
import org.mule.extensions.jms.internal.operation.JmsAcknowledge;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Set;

/**
 * Errors that can be thrown in the {@link JmsAcknowledge#recoverSession(String)} operation.
 *
 * @since 4.0
 */
public class JmsSessionRecoverErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(SESSION_RECOVER)
        .build();
  }
}

