/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.exception;

import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.config.internal.error.ErrorTypeBuilder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.core.privileged.exception.MessagingExceptionUtils;

public class MessagingExceptionUtilsDelegate {

  private static final Exception EXPECTED_EXCEPTION = new Exception("Expected");

  public static EventProcessingException createMessagingException(CoreEvent event, ErrorTypeRepository errorTypeRepository) {
    final CoreEvent eventWithError = CoreEvent.builder(event).error(ErrorBuilder.builder(EXPECTED_EXCEPTION)
        .errorType(ErrorTypeBuilder.builder().namespace("TEST").identifier("EXPECTED")
            .parentErrorType(errorTypeRepository.getAnyErrorType())
            .build())
        .build())
        .build();

    return MessagingExceptionUtils.createMessagingException(eventWithError,
                                                            new ComponentExecutionException(EXPECTED_EXCEPTION, event));
  }

}
