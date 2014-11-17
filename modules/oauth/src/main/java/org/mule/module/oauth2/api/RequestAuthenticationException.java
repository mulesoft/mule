/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.api;

import org.mule.api.DefaultMuleException;
import org.mule.config.i18n.Message;

/**
 * Exception throw when it's not possible to create the authentication request for a given request.
 */
public class RequestAuthenticationException extends DefaultMuleException
{

    public RequestAuthenticationException(Message message)
    {
        super(message);
    }

}
