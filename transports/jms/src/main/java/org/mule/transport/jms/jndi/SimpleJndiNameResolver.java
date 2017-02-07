/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.jndi;

import org.mule.api.lifecycle.InitialisationException;

import javax.naming.CommunicationException;
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
        try
        {
            return doLookUp(name);
        }
        catch (CommunicationException e)
        {
            jndiContext = this.createInitialContext();
            return doLookUp(name);
        }
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
    
    private Object doLookUp(String name) throws NamingException
    {
        return jndiContext.lookup(name);
    }

}
