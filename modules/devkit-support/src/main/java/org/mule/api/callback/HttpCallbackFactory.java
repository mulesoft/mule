/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.callback;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.security.oauth.callback.HttpCallbackAdapter;
import org.mule.security.oauth.processor.FetchAccessTokenMessageProcessor;

public interface HttpCallbackFactory
{

    public HttpCallback createCallback(HttpCallbackAdapter adapter,
                                       String authCodeRegex,
                                       FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor,
                                       MessageProcessor listener,
                                       MuleContext muleContext,
                                       FlowConstruct flowConstruct) throws MuleException;

}
