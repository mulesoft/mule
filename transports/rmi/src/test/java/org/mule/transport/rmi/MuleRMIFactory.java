/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.rmi;

import org.mule.jndi.MuleInitialContextFactory;
import org.mule.tck.services.MatchingMethodsComponent;
import org.mule.tck.services.SimpleMathsComponent;

import javax.naming.InitialContext;

public class MuleRMIFactory
{

    public Object create() throws Exception
    {
        InitialContext ic = new InitialContext();
        ic.addToEnvironment(InitialContext.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());

        // Bind our service object
        ic.bind("SimpleMathsUMO", new SimpleMathsComponent());
        ic.bind("MatchingUMO", new MatchingMethodsComponent());
        ic.bind("TestService", new MatchingMethodsComponent());
        return ic;
    }

}
