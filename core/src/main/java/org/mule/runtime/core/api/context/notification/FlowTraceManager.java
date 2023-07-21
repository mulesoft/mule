/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;

/**
 * Provides callbacks for notifying when a flow call from another flow is started or completed.
 * 
 * @since 3.8.0
 */
@NoImplement
public interface FlowTraceManager {

  /**
   * Handles the start of the passed flowName for the given event.
   * 
   * @param notificationInfo the notification information about the event for which the flow is being started
   * @param flowName         the name of the flow that is about to start
   */
  void onFlowStart(EnrichedNotificationInfo notificationInfo, String flowName);

  /**
   * Handles the completion of the current flow for the given event.
   * 
   * @param notificationInfo the notification information about the event for which the flow is being completed
   */
  void onFlowComplete(EnrichedNotificationInfo notificationInfo);

}
