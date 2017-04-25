/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import static org.mule.extensions.jms.api.exception.JmsErrors.ACK;
import static org.mule.extensions.jms.api.exception.JmsErrors.CONSUMING;
import static org.mule.extensions.jms.api.exception.JmsErrors.DESTINATION_NOT_FOUND;
import static org.mule.extensions.jms.api.exception.JmsErrors.TIMEOUT;
import org.mule.extensions.jms.api.config.ConsumerAckMode;
import org.mule.extensions.jms.internal.config.JmsConfig;
import org.mule.extensions.jms.internal.connection.JmsTransactionalConnection;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.internal.operation.JmsConsume;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Errors that can be thrown in the
 * {@link JmsConsume#consume(JmsTransactionalConnection, JmsConfig, String, ConsumerType, ConsumerAckMode, String, String, String, Long, TimeUnit)}
 * operation operation.
 *
 * @since 4.0
 */
public class JmsConsumeErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(CONSUMING)
        .add(TIMEOUT)
        .add(DESTINATION_NOT_FOUND)
        .add(ACK)
        .build();
  }
}

