/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
