/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.events;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.mule.TestConnector;

public class TestConnectorWithContainerDependents extends TestConnector
{
    private Apple containerProp;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transport.AbstractConnector#doInitialise()
     */
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        if (containerProp == null)
        {
            throw new IllegalStateException(
                "Initialise should not be called before all properties have been set");
        }

    }

    public Apple getContainerProp()
    {
        return containerProp;
    }

    public void setContainerProp(Apple containerProp)
    {
        this.containerProp = containerProp;
    }

}
