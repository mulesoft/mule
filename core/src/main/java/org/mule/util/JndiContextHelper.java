/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Common code for initialising the JNDI context.
 */
public final class JndiContextHelper
{
    /** Do not instanciate. */
    private JndiContextHelper ()
    {
        // no-op
    }

    /**
     * Create a new initial context.
     * 
     * @param environment JNDI properties or <code>null</code>. In the latter case
     *            a default constructor of <code>InitialContext</code> will be
     *            called with standard JNDI lookup properties semantics.
     * @return jndi context
     * @throws NamingException if there was a JNDI error
     */
    public static Context initialise(final Map environment) throws NamingException
    {
        Context context;
        if (environment != null && environment.size() > 0)
        {
            context = new InitialContext(new Hashtable(environment));
        }
        else
        {
            context = new InitialContext();
        }

        return context;
    }
}
