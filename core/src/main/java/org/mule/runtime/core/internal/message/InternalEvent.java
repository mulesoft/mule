/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Map;

public interface InternalEvent extends PrivilegedEvent {

  /**
   * Internal parameters used by the runtime to pass information around.
   *
   */
  Map<String, ?> getInternalParameters();

  /**
   * Returns the muleContext for the Mule node that this event was received in
   *
   * @return the muleContext for the Mule node that this event was received in
   * @deprecated TODO MULE-10013 remove this
   */
  @Deprecated
  MuleContext getMuleContext();

  /**
   * Retrieves the service for the current event
   *
   * @return the service for the event
   * @deprecated TODO MULE-10013 remove this
   */
  @Deprecated
  FlowConstruct getFlowConstruct();

}
