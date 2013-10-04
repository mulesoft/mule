/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.callback;

/**
 * Callback returned by methods that are annotated with @Source
 * <p/>
 * It will be executed when the MessageSource is being stopped.
 */
public interface StopSourceCallback
{

    void stop() throws Exception;
}
