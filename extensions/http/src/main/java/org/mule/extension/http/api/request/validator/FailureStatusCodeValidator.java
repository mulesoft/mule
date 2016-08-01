/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleContext;

/**
 * Response validator that allows specifying which status codes should be treated as failures.
 * Responses with such status codes will cause the component to throw an exception.
 *
 * @since 4.0
 */
public class FailureStatusCodeValidator extends RangeStatusCodeValidator
{

    @Override
    public void validate(MuleMessage responseMessage, MuleContext context) throws ResponseValidatorException
    {
        int status = ((HttpResponseAttributes) responseMessage.getAttributes()).getStatusCode();

        if (belongs(status))
        {
            throw new ResponseValidatorException(String.format("Response code %d mapped as failure", status), responseMessage, context);
        }
    }

}