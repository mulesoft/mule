/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.exception;

import org.mule.api.connection.ConnectionException;
import org.mule.extension.api.introspection.ExceptionEnricher;

public class HeisenbergConnectionExceptionEnricher implements ExceptionEnricher
{
    public static final String ENRICHED_MESSAGE = "Enriched Connection Exception: ";

    @Override
    public Exception enrichException(Exception e)
    {
        return new ConnectionException(ENRICHED_MESSAGE + e.getMessage());
    }
}
