/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.callback;

import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * Base interface for components that publish a http callback.
 */
public interface HttpCallback extends Startable, Stoppable, MuleContextAware
{

    /**
     * @return the url that the callback is listening on
     */
    String getUrl();
}
