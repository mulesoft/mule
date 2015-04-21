/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.processor.MessageProcessor;

/**
 * Marker interface used to mark the {@link org.mule.api.processor.InterceptingMessageProcessor} and
 * {@link org.mule.api.processor.MessageRouter} that support non-blocking processing.  Unsupported implementations will
 * cause Mule to fall back to synchronous blocking processing.  Note: All standard {@link org.mule.api.processor.MessageProcessor}
 * implementations that aren't one of the two types mentioned above are implicitly supported.
 */
public interface NonBlockingSupported
{

}
