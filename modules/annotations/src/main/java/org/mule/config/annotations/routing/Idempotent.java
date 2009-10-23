/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations.routing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <code>IdempotentReceiver</code> ensures that only unique messages are received
 * by a component. There are different types of idempotency handlers:
 * <ul>
 * <li>ID - Checks the unique ID of the incoming message. Note that the underlying
 * endpoint must support unique message Ids for this to
 * work, otherwise a <code>UniqueIdNotSupportedException</code> is thrown.</li>
 * <li>HASH - Calculates the SHA-256 hash of the message itself. This provides a
 * value with an infinitesimally small chance of a collision.</li>
 * </ul>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Router(type = RouterType.Inbound)
public @interface Idempotent
{
    /**
     * The message id expression to index the received messages against
     * @return The message id expression to index the received messages against
     */
    public String value() default "#[mule:messageId]";

}
