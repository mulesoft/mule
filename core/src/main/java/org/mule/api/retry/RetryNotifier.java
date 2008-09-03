/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.retry;

/**
 * The RetryNotifier interface is a callback that allows actions to be performed after each retry. For example, when
 * retrying connections Mule will fire server notification events on success or failure.
 */

public interface RetryNotifier
{
    public void sucess(RetryContext context);

    public void failed(RetryContext context, Throwable e);
}
