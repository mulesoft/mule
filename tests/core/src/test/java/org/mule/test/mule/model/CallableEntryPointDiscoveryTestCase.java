/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.mule.model;

import org.mule.model.CallableEntryPointResolver;
import org.mule.tck.model.AbstractEntryPointDiscoveryTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CallableEntryPointDiscoveryTestCase extends AbstractEntryPointDiscoveryTestCase
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.model.AbstractEntryPointDiscoveryTestCase#getComponentMappings()
     */
    public ComponentMethodMapping[] getComponentMappings()
    {
        ComponentMethodMapping[] mappings = new ComponentMethodMapping[2];
        mappings[0] = new ComponentMethodMapping(WaterMelon.class, "myEventHandler", UMOEvent.class, true);
        mappings[1] = new ComponentMethodMapping(Apple.class, "onCall", UMOEventContext.class, false);
        return mappings;
    }

    public UMOEntryPointResolver getEntryPointResolver()
    {
        return new CallableEntryPointResolver();
    }
}
