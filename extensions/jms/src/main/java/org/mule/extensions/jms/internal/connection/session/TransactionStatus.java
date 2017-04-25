/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.session;

/**
 * Enum to indicate the current status of a transaction over a session.
 * <ul>
 *     <li>{@link TransactionStatus#NONE} means that there is no started transaction for the current {@link Thread}</li>
 *     <li>{@link TransactionStatus#STARTED} means that there is a transaction being executed in the current {@link Thread}</li>
 * </ul>
 *
 * @since 4.0
 */
public enum TransactionStatus {
  NONE, STARTED
}
