/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event.context;

import org.mule.runtime.core.internal.execution.FlowProcessMediator;
import org.mule.runtime.core.internal.message.EventInternalContext;

/**
 * Context required by {@link FlowProcessMediator} to handle flow responses.
 *
 * @since 4.4, 4.3.1
 */
public interface FlowProcessMediatorContext extends EventInternalContext<FlowProcessMediatorContext> {

}
