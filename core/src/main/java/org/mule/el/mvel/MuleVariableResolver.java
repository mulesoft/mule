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

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.SimpleSTValueResolver;

class MuleVariableResolver extends SimpleSTValueResolver
{
    private static final long serialVersionUID = -4957789619105599831L;
    private String name;

    public MuleVariableResolver(String name, Object value, Class<?> type)
    {
        super(value, type);
    }

    @Override
    public String getName()
    {
        return name;
    }

    public Object getValue(VariableResolverFactory variableResolverFactory)
    {
        return getValue();
    }
}
