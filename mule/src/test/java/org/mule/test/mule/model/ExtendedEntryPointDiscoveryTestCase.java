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

import java.util.EventObject;

import org.mule.model.DynamicEntryPointResolver;
import org.mule.tck.model.AbstractEntryPointDiscoveryTestCase;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitLover;
import org.mule.tck.testmodels.fruit.ObjectToFruitLover;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ExtendedEntryPointDiscoveryTestCase extends AbstractEntryPointDiscoveryTestCase
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.model.AbstractEntryPointDiscoveryTestCase#getComponentMappings()
     */
    public ComponentMethodMapping[] getComponentMappings()
    {
        ComponentMethodMapping[] mappings = new ComponentMethodMapping[3];
        mappings[0] = new ComponentMethodMapping(WaterMelon.class, "myEventHandler", UMOEvent.class);
        mappings[1] = new ComponentMethodMapping(FruitBowl.class, "consumeFruit", FruitLover.class);
        mappings[2] = new ComponentMethodMapping(Banana.class, "peelEvent", EventObject.class);
        return mappings;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.model.AbstractEntryPointDiscoveryTestCase#getDescriptorToResolve(java.lang.String)
     */
    public UMODescriptor getDescriptorToResolve(String className) throws Exception
    {
        UMODescriptor descriptor = super.getDescriptorToResolve(className);
        if (className.equals(FruitBowl.class.getName())) {
            UMOEndpoint endpoint = descriptor.getOutboundEndpoint();
            endpoint.setType(UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);
            endpoint.setTransformer(new ObjectToFruitLover());
            descriptor.setInboundEndpoint(endpoint);
        }
        return descriptor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.model.AbstractEntryPointDiscoveryTestCase#getEntryPointResolver()
     */
    public UMOEntryPointResolver getEntryPointResolver()
    {
        return new DynamicEntryPointResolver();
    }

}
