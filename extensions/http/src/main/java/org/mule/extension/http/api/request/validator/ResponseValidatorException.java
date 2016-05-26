/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.config.i18n.CoreMessages;

/**
 * Signals that an error occurred while validating a {@link MuleMessage}
 *
 * @since 4.0
 */
public class ResponseValidatorException extends MessagingException
{

    public ResponseValidatorException(String message, MuleMessage muleMessage)
    {
        super(CoreMessages.createStaticMessage(message), (DefaultMuleMessage) muleMessage);
    }
}
