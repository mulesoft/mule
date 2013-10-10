/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
