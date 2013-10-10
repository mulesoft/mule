/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.test;

import org.mule.tck.jndi.TestContextFactory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

public class JmsTestContextFactory extends TestContextFactory
{
    public static boolean failWhenRetrievingInitialContext = false;

    @Override
    protected void populateTestData(Context context) throws NamingException
    {
        super.populateTestData(context);
        context.bind("jms/connectionFactory", new TestConnectionFactory());
    }

    @Override
    public Context getInitialContext(Hashtable environment) throws NamingException
    {
        if (failWhenRetrievingInitialContext)
        {
            throw new NamingException("Initial context not ready");
        }
        return super.getInitialContext(environment);
    }

    @Override
    public Context getInitialContext() throws NamingException
    {
        if (failWhenRetrievingInitialContext)
        {
            throw new NamingException("Initial context not ready");
        }
        return super.getInitialContext();
    }
}
