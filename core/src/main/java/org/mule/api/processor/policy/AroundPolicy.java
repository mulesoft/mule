/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor.policy;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 *
 */
public interface AroundPolicy
{
    String getName();

    MuleEvent invoke(PolicyInvocation invocation) throws MuleException;
}
