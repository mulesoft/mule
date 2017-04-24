/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp.exception;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * <code>AmbiguousPGPPrincipalException</code> is the exception in case exists the same principal
 * in more than one public key.
 */
public class AmbiguousPGPPrincipalException extends MuleException
{

    public AmbiguousPGPPrincipalException(Message message)
    {
        super(message);
    }

}
