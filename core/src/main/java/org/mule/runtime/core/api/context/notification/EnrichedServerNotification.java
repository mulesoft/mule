/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.message.GroupCorrelation;

import java.util.Map;
import java.util.Optional;

/**
 * This notification type includes information about the event, exception and flow where it occurred.
 */
public abstract class EnrichedServerNotification extends ServerNotification {

  protected EnrichedNotificationInfo notificationInfo;

  public EnrichedServerNotification(EnrichedNotificationInfo notificationInfo, int action, String resourceIdentifier) {
    super(notificationInfo, action, resourceIdentifier);
    this.notificationInfo = notificationInfo;
  }

  public EnrichedServerNotification(EnrichedNotificationInfo notificationInfo, int action, ComponentLocation componentLocation) {
    this(notificationInfo, action, componentLocation != null ? componentLocation.getRootContainerName() : null);
  }

  /**
   * This function should not be used anymore, try getMessage, getError or getException depending on the situation.
   *
   * @return the notification information instead of the event used before
   */
  @Deprecated
  @Override
  public Object getSource() {
    return notificationInfo;
  }

  public String getUniqueId() {
    return notificationInfo.getUniqueId();
  }

  public String getCorrelationId() {
    return notificationInfo.getCorrelationId();
  }

  public Optional<GroupCorrelation> getGroupCorrelation() {
    return notificationInfo.getGroupCorrelation();
  }

  public Message getMessage() {
    return notificationInfo.getMessage();
  }

  public Optional<Error> getError() {
    return notificationInfo.getError();
  }

  public AnnotatedObject getComponent() {
    return notificationInfo.getComponent();
  }

  public FlowCallStack getFlowCallStack() {
    return notificationInfo.getFlowCallStack();
  }

  public Exception getException() {
    return notificationInfo.getException();
  }

  public Map<String, TypedValue> getVariables() {
    return notificationInfo.getVariables();
  }

  public String getOriginatingFlowName() {
    return notificationInfo.getOriginatingFlowName();
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier + ", serverId=" + serverId
        + ", timestamp=" + timestamp + "}";
  }

  public String getLocationUri() {
    if (getComponent() == null) {
      return null;
    }

    ComponentLocation location = getComponent().getLocation();
    return location.getRootContainerName() + "/" + location.getComponentIdentifier().getIdentifier().toString();
  }

  public EnrichedNotificationInfo getInfo() {
    return notificationInfo;
  }
}
