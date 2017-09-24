/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
