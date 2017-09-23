/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.core.api.MuleContext;

/**
 * <code>MuleContextNotification</code> is fired when an event such as the mule context starting occurs. The payload of this event
 * will always be a reference to the muleContext.
 */
public class MuleContextNotification extends AbstractServerNotification {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -3246036188011581121L;

  public static final int CONTEXT_INITIALISING = CONTEXT_EVENT_ACTION_START_RANGE + 1;
  public static final int CONTEXT_INITIALISED = CONTEXT_EVENT_ACTION_START_RANGE + 2;
  public static final int CONTEXT_STARTING = CONTEXT_EVENT_ACTION_START_RANGE + 3;
  public static final int CONTEXT_STARTED = CONTEXT_EVENT_ACTION_START_RANGE + 4;
  public static final int CONTEXT_STOPPING = CONTEXT_EVENT_ACTION_START_RANGE + 5;
  public static final int CONTEXT_STOPPED = CONTEXT_EVENT_ACTION_START_RANGE + 6;
  public static final int CONTEXT_DISPOSING = CONTEXT_EVENT_ACTION_START_RANGE + 7;
  public static final int CONTEXT_DISPOSED = CONTEXT_EVENT_ACTION_START_RANGE + 8;

  static {
    registerAction("mule context initialising", CONTEXT_INITIALISING);
    registerAction("mule context initialised", CONTEXT_INITIALISED);
    registerAction("mule context starting", CONTEXT_STARTING);
    registerAction("mule context started", CONTEXT_STARTED);
    registerAction("mule context stopping", CONTEXT_STOPPING);
    registerAction("mule context stopped", CONTEXT_STOPPED);
    registerAction("mule context disposing", CONTEXT_DISPOSING);
    registerAction("mule context disposed", CONTEXT_DISPOSED);
  }

  private String clusterId;
  private String domain;
  private transient MuleContext muleContext;

  public MuleContextNotification(MuleContext context, String action) {
    this(context, getActionId(action));
  }

  public MuleContextNotification(MuleContext context, int action) {
    super(context.getId(), action);
    this.muleContext = context;
    this.resourceIdentifier = context.getConfiguration().getId();
    this.clusterId = context.getClusterId();
    this.domain = context.getConfiguration().getDomainId();
  }

  public String getClusterId() {
    return clusterId;
  }

  public String getDomain() {
    return domain;
  }

  public MuleContext getMuleContext() {
    return this.muleContext;
  }

  @Override
  public String toString() {
    return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier + ", timestamp="
        + timestamp + "}";
  }

  @Override
  public boolean isSynchronous() {
    return true;
  }
}
