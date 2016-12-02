/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.functional.Either;

import java.util.Map;

/**
 * Result of executing a {@link SourcePolicy}.
 *
 * It contains the {@link Event} result of the flow execution and also allows to generate
 * the response parameters and error response parameters to be sent by the source.
 *
 * @since 4.0
 */
public interface SourcePolicyResult
{

    Either<MessagingException, Event> getResult();

    /**
     * @return the response parameters to be sent by the source.
     */
    Map<String, Object> getResponseParameters();

    /**
     * @param failureEvent the failure event used to generate the error response parameters.
     * @return the response parameters to be sent by the source in case of a failure.
     */
    Map<String, Object> getErrorResponseParameters(Event failureEvent);

}
