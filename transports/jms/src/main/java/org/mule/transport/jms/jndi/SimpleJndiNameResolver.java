/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.jndi;

import org.mule.api.lifecycle.InitialisationException;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Defines a simple {@link JndiNameResolver} that maintains a {@link Context}
 * instance opened all the time and always relies on the context to do the look
 * ups.
 */
public class SimpleJndiNameResolver extends AbstractJndiNameResolver
{

    // @GuardedBy(this)
    private Context jndiContext;

    public synchronized Object lookup(String name) throws NamingException
    {
        return jndiContext.lookup(name);
    }

    public void initialise() throws InitialisationException
    {
        if (jndiContext == null)
        {
            try
            {
                jndiContext = createInitialContext();
            }
            catch (NamingException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    @Override
    public void dispose()
    {
        if (jndiContext != null)
        {
            try
            {
                jndiContext.close();
            }
            catch (NamingException e)
            {
                logger.error("Jms connector failed to dispose properly: ", e);
            }
            finally
            {
                jndiContext = null;
            }
        }
    }
}
