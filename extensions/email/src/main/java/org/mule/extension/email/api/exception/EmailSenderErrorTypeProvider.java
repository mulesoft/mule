/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.api.exception.EmailError.ATTACHMENT;
import org.mule.extension.email.internal.sender.EmailSettings;
import org.mule.extension.email.internal.sender.SMTPConfiguration;
import org.mule.extension.email.internal.sender.SenderConnection;
import org.mule.extension.email.internal.sender.SendOperation;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Errors that can be thrown in the {@link SendOperation#send(SenderConnection, SMTPConfiguration, EmailSettings)} operation.
 * 
 * @since 4.0
 */
public class EmailSenderErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(ATTACHMENT)
        .build();
  }
}

