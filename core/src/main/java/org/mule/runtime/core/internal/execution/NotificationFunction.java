/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.notification.Notification;

import java.util.function.BiFunction;

/**
 * A {@link java.util.function.BiFunction} that generates {@link Notification Notifications} from a given {@link Event} and a
 * given {@link Component}.
 *
 * @since 4.1
 */
@FunctionalInterface
public interface NotificationFunction extends BiFunction<Event, Component, Notification> {

}
