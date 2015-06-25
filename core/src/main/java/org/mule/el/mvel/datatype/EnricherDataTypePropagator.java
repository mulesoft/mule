/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.MuleMessage;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.transformer.types.TypedValue;

/**
 * Propagates {@link org.mule.api.transformer.DataType} from the right to the left side
 * of an assignment expression if possible
 */
public interface EnricherDataTypePropagator
{

    /**
     * Propagates {@link org.mule.api.transformer.DataType} on message enrichment
     *
     * @param message message being enriched
     * @param typedValue value used to enrich the message
     * @param compiledExpression assignment expression used for enrichment
     * @return true if propagation was done, false otherwise
     */
    boolean propagate(MuleMessage message, TypedValue typedValue, CompiledExpression compiledExpression);
}
