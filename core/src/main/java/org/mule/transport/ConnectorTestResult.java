/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.common.TestResult;

public class ConnectorTestResult implements TestResult
{

    private Status status;
    private String message;

    public ConnectorTestResult(TestResult.Status status)
    {
        this(status, "");
    }

    public ConnectorTestResult(TestResult.Status status, String message)
    {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public Status getStatus()
    {
        return status;
    }

}
