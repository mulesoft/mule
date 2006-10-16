/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.rmi;

import java.util.Map;

import javax.naming.InitialContext;

import org.mule.impl.jndi.MuleInitialContextFactory;
import org.mule.tck.services.MatchingMethodsComponent;
import org.mule.tck.services.SimpleMathsComponent;

public class MuleRMIFactory implements org.mule.config.PropertyFactory
{

    public Object create(Map properties) throws Exception
    {
        InitialContext ic = new InitialContext();
        ic.addToEnvironment(InitialContext.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());

        // Bind our service object
        ic.bind("SimpleMathsUMO", new SimpleMathsComponent());
        ic.bind("MatchingUMO", new MatchingMethodsComponent());
        return ic;
    }

}
