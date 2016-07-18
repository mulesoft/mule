/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.Correlation;

public enum CorrelationMode
{
    IF_NOT_SET, ALWAYS, NEVER;

    /**
     * @param message the message to check for its correlation attributes.
     * @return whether correlation has to be handled for the message that has this {@link Correlation}.
     */
    public boolean doCorrelation(MuleMessage message)
    {
        return this != NEVER
               && ((!message.getCorrelation().getId().isPresent() && this == IF_NOT_SET)
                   || this == ALWAYS);
    }

}
