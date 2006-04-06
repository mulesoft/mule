/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.test.mule.model;

import org.mule.impl.RequestContext;
import org.mule.model.DynamicEntryPointResolver;
import org.mule.model.TooManySatisfiableMethodsException;
import org.mule.model.NoSatisfiableMethodsException;
import org.mule.model.CallableEntryPointResolver;
import org.mule.tck.model.AbstractEntryPointDiscoveryTestCase;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.ObjectToFruitLover;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.config.MuleProperties;

import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 *
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

    public UMOEntryPointResolver getEntryPointResolver() {
        return new CallableEntryPointResolver();
    }
}
