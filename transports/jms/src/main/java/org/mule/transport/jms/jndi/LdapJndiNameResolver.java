/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.jndi;

import javax.naming.CommunicationException;
import javax.naming.NamingException;

/**
 * This resolver is used to provide the ldap jndi connection the possibility 
 * to recover connection when an ldap is
 * restarted.
 */
public class LdapJndiNameResolver extends SimpleJndiNameResolver
{

    @Override
    public synchronized Object lookup(String name) throws NamingException
    {
        try
        {
            return jndiContext.lookup(name);
        }
        catch (CommunicationException e)
        {
            jndiContext = this.createInitialContext();
            return doLookUp(name);
        }
    }

    private Object doLookUp(String name) throws NamingException
    {
        return jndiContext.lookup(name);
    }

}
