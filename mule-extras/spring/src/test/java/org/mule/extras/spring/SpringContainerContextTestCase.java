/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.extras.spring;

import org.mule.tck.model.AbstractComponentResolverTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.model.UMOContainerContext;
import org.mule.umo.model.ComponentNotFoundException;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SpringContainerContextTestCase extends AbstractComponentResolverTestCase
{
    SpringContainerContext context;

    /* (non-Javadoc)
     * @see org.mule.tck.model.AbstractComponentResolverTestCase#getConfiguredResolver()
     */
    public UMOContainerContext getContainerContext()
    {
        return context;
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        context = new SpringContainerContext();
        context.setConfigFile("test-application-context.xml");
    }

    public void testContainerContext()
    {
        UMOContainerContext container = getContainerContext();

        assertNotNull(container);

        Object result = null;

        try
        {
            result = container.getComponent(null);
            fail("Should throw ComponentNotFoundException for null key");
        }
        catch (ComponentNotFoundException e)
        {
            // expected
        }

        try
        {
            result = container.getComponent("abcdefg123456!£$%^n");
            fail("Should throw ComponentNotFoundException for a key that doesn't exist");
        }
        catch (ComponentNotFoundException e)
        {
            // expected
        }

        try
        {
            result = container.getComponent(Apple.class.getName());
            assertNotNull("Component should exist in container", result);
        } catch (ComponentNotFoundException e)
        {
            fail("Component should exist in the container");
        }
    }

}
