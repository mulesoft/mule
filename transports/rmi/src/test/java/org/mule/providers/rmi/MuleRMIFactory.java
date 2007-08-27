/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.rmi;

import org.mule.impl.jndi.MuleInitialContextFactory;
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
