/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.MuleManager;
import org.mule.registry.RegistryException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.model.UMOModel;

import java.util.Iterator;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 * @version $Revision$
 */
public class MuleModelTestCase extends AbstractMuleTestCase
{

    public void testDescriptorAlreadyDefinedThrowsException() throws Exception
    {
        final String descriptorName = "TEST_COMPONENT_1";
        MuleDescriptor descriptor = getTestDescriptor(descriptorName, "java.lang.Object");
        MuleDescriptor duplicateDescriptor = getTestDescriptor(descriptorName, "java.lang.Object");
        final UMOModel model = getDefaultModel();
        MuleManager.getRegistry().registerComponent(descriptor, model.getName());
        try
        {
            // register it again with the same name
            MuleManager.getRegistry().registerComponent(duplicateDescriptor, model.getName());
            fail("Trying to register a component descriptor with the same name "
                 + "must have thrown an exception.");
        }
        catch (RegistryException e)
        {
            // expected
            final String message = e.getMessage();
            assertTrue("Exception message should contain our descriptor name.",
                (message.indexOf("\"" + descriptorName + "\"") > -1));
        }

        // count components (no direct method to count 'em)
        int componentCount = 0;
        for (Iterator it = model.getComponentNames(); it.hasNext();)
        {
            it.next();
            componentCount++;
        }

        assertEquals("Wrong number of components registered in the model.", 1, componentCount);
    }
}
