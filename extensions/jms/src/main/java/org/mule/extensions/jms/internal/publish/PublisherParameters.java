/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.publish;

import java.util.concurrent.TimeUnit;

import javax.jms.Message;

/**
 * Declares the parameters that can override the default values for publishing a {@link Message}
 *
 * @since 4.0
 */
public interface PublisherParameters {

  Boolean isPersistentDelivery();

  Integer getPriority();

  Long getTimeToLive();

  TimeUnit getTimeToLiveUnit();

  Boolean isDisableMessageId();

  Boolean isDisableMessageTimestamp();

  Long getDeliveryDelay();

  TimeUnit getDeliveryDelayUnit();
}
