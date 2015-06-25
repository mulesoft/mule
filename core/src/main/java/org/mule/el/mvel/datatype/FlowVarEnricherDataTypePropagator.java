/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.transport.PropertyScope;

/**
 * Propagates data type for flow vars used for enrichment target
 */
public class FlowVarEnricherDataTypePropagator extends AbstractVariableEnricherDataTypePropagator
{

    public FlowVarEnricherDataTypePropagator()
    {
        super("flowVars", PropertyScope.INVOCATION);
    }
}
