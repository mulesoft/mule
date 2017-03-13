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
import static org.mule.extensions.jms.api.exception.JmsErrors.ILLEGAL_BODY;
import static org.mule.extensions.jms.api.exception.JmsErrors.PUBLISHING;
import static org.mule.extensions.jms.api.exception.JmsErrors.TIMEOUT;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.message.MessageBuilder;
import org.mule.extensions.jms.api.operation.JmsPublishConsume;
import org.mule.extensions.jms.internal.publish.JmsPublishParameters;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Errors that can be thrown in the
 * {@link JmsPublishConsume#publishConsume(JmsConfig, JmsConnection, String, MessageBuilder, AckMode, long, TimeUnit, JmsPublishParameters, String, String)}
 * operation operation.
 *
 * @since 4.0
 */
public class JmsPublishConsumeErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(PUBLISHING)
        .add(ILLEGAL_BODY)
        .add(CONSUMING)
        .add(TIMEOUT)
        .add(DESTINATION_NOT_FOUND)
        .add(ACK)
        .build();
  }
}

