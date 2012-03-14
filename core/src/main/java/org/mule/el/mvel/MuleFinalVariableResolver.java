/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.config.i18n.CoreMessages;

import org.mvel2.ImmutableElementException;

class MuleFinalVariableResolver extends MuleVariableResolver
{
    private static final long serialVersionUID = -4957789619105599831L;
    private String name;

    public MuleFinalVariableResolver(String name, Object value, Class<?> type)
    {
        super(name, value, type);
    }

    @Override
    public void setValue(Object value)
    {
        throw new ImmutableElementException(CoreMessages.expressionFinalVariableCannotBeAssignedValue(name)
            .getMessage());
    }
}
