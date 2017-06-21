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
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.GroupCorrelation;

import java.util.Map;
import java.util.Optional;

/**
 * This notification type includes information about the event, exception and flow where it occurred.
 */
public abstract class EnrichedServerNotification extends ServerNotification {

  protected EnrichedNotificationInfo notificationInfo;
  protected FlowConstruct flowConstruct;

  public EnrichedServerNotification(EnrichedNotificationInfo notificationInfo, int action,
                                    FlowConstruct flowConstruct) {
    this(notificationInfo, action, flowConstruct != null ? flowConstruct.getName() : null);
    this.flowConstruct = flowConstruct;
  }

  public EnrichedServerNotification(EnrichedNotificationInfo notificationInfo, int action,
                                    String resourceIdentifier) {
    super(notificationInfo, action, resourceIdentifier);
    this.notificationInfo = notificationInfo;
  }

  /**
   * This function should not be used anymore, try getMessage, getError or getException depending
   * on the situation.
   *
   * @return the notification information instead of the event used before
   */
  @Deprecated
  @Override
  public Object getSource() {
    return notificationInfo;
  }

  /**
   * This function should not be used anymore, try getFlowConstruct().getName() or getComponent().getLocation() depending
   * on the situation.
   *
   * @return the resource identifier used before
   */
  @Deprecated
  @Override
  public String getResourceIdentifier() {
    return super.getResourceIdentifier();
  }

  public String getUniqueId() {
    return notificationInfo.getUniqueId();
  }

  public String getCorrelationId() {
    return notificationInfo.getCorrelationId();
  }

  public GroupCorrelation getGroupCorrelation() {
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

  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }

  @Override
  public String toString() {
    //return format("%s {action=%s, resourceId=%s, serverId=%s, timestamp=%s, uniqueId=%s, correlationId=%s, groupCorrelation=%s")
    return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier + ", serverId=" + serverId
        + ", timestamp=" + timestamp + "}";
  }

  public String getLocationUri() {
    if (getComponent() == null) {
      return null;
    }

    ComponentLocation location = getComponent().getLocation();
    return location.getParts().get(0).getPartPath() + "/" + location.getComponentIdentifier().getIdentifier().toString();
  }

  public EnrichedNotificationInfo getInfo() {
    return notificationInfo;
  }
}
