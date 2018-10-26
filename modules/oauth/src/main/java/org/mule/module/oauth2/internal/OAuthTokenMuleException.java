/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.DefaultMuleException;

/**
 * Standard exception wrapper for token response processing problems. 
 * 
 * @since 3.10
 */
public class OAuthTokenMuleException extends DefaultMuleException
{

    private static final long serialVersionUID = 4742375792184017374L;

    public OAuthTokenMuleException(Throwable cause)
    {
        super(cause);
    }

}
