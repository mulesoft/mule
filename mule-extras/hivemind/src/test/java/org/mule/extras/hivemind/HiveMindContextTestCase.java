/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Massimo Lusetti. All rights reserved.
 * http://www.datacode.it
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.extras.hivemind;

import org.apache.hivemind.Registry;
import org.mule.tck.model.AbstractContainerContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * @author <a href="mailto:massimo@datacode.it">Massimo Lusetti</a>
 * @version $Revision$
 */
public class HiveMindContextTestCase extends AbstractContainerContextTestCase
{

    HiveMindContext context;

    protected void doSetUp() throws Exception
    {
	context = new HiveMindContext();
	context.initialise();
    }
    /*
    * (non-Javadoc)
    *
    * @see org.mule.tck.model.AbstractComponentResolverTestCase#getConfiguredResolver()
    */
    public UMOContainerContext getContainerContext()
    {
/*
    	    Registry registry = context.getRegistry();
    	    
        boolean x = registry.containsService(FruitBowl.class);
        System.err.println("Contiene FruitBowl? " + x);
        FruitBowl bowl = (FruitBowl) registry.getService(FruitBowl.class);
        System.err.println("bowl contiene: " + bowl.toString());
        System.err.println("bowl contiene: " + bowl.getApple());
        
        
*/
        return context;
    }

    public void testContainerNotNull() throws Exception
    {
	assertNotNull(getContainerContext());
    }

    public void testFruitBowl() throws Exception
    {
	FruitBowl result = null;
	try {
            result = (FruitBowl) context.getComponent(FruitBowl.class.getName());
            assertNotNull("Component FruitBwol should exist in container", result);
            Apple apple = result.getApple();
            assertNotNull("Component Apple should be in FruitBowl", apple);
        } catch (ObjectNotFoundException e) {
            fail("Component should exist in the container");
        }
    }

    
}
