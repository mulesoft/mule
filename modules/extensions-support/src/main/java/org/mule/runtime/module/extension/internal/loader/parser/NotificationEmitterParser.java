/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.notification.NotificationModel;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @since 4.5.0
 */
public interface NotificationEmitterParser {

  List<NotificationModel> getEmittedNotifications(Function<String, Optional<NotificationModel>> notificationMapper);

}
