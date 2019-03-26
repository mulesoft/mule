/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

/**
 * Declares the kind of Streaming Strategies supported.
 *
 * There are three strategies:
 *
 * <ul>
 * <li><b>IN_MEMORY</b>: Repeatable streaming strategy that stores read information in memory for future reads.</li>
 * <li><b>NON_REPEATABLE</b>: Non repeatable streaming strategy.</li>
 * <li><b>FILE_STORE</b>: Repeatable streaming strategy that persists the read information for future reads.</li>
 * </ul>
 *
 * @since 4.2
 */
public enum StreamingStrategy {
  IN_MEMORY, NON_REPEATABLE, FILE_STORE
}
