/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.component;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * The scope of the MuleEvent Callback is to be able to get a the message currently being processed by the
 * {@link FunctionalTestProcessor} and make assertions on the message payload, headers or attachments or to make changes required
 * for the test.
 *
 * @see FunctionalTestProcessor
 */
public interface EventCallback {

  void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception;
}
