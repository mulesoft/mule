/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring;

import org.mule.tck.model.AbstractContainerContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SpringContainerContextTestCase extends AbstractContainerContextTestCase
{
    SpringContainerContext context;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.model.AbstractComponentResolverTestCase#getConfiguredResolver()
     */
    public UMOContainerContext getContainerContext()
    {
        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        context = new SpringContainerContext();
        context.setConfigFile("test-application-context.xml");
    }

    public void testContainerContext() throws Exception
    {
        UMOContainerContext container = getContainerContext();
        container.initialise();
        assertNotNull(container);

        try {
            container.getComponent(null);
            fail("Should throw ObjectNotFoundException for null key");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            container.getComponent("abcdefg123456!£$%^n");
            fail("Should throw ObjectNotFoundException for a key that doesn't exist");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            Object result = container.getComponent(Apple.class.getName());
            assertNotNull("Component should exist in container", result);
        } catch (ObjectNotFoundException e) {
            fail("Component should exist in the container");
        }
    }

}
