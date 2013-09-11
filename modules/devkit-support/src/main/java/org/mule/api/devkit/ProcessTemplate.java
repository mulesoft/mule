/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.devkit;

import org.mule.security.oauth.callback.ProcessCallback;

public interface ProcessTemplate<T, O>
{

    public T execute(ProcessCallback<T, O> callback,
                     org.mule.api.processor.MessageProcessor messageProcessor,
                     org.mule.api.MuleEvent event) throws Exception;

    public T execute(ProcessCallback<T, O> callback,
                     org.mule.api.routing.filter.Filter filter,
                     org.mule.api.MuleMessage message) throws Exception;
}
