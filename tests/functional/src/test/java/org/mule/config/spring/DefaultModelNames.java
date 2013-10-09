/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.model.Model;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultModelNames extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "default-model-names.xml";
    }

    @Test
    public void testNames()
    {
        Collection models = muleContext.getRegistry().lookupObjects(Model.class);
        assertEquals(3, models.size()); // includes system model
        Set modelNames = new HashSet();
        for (Iterator each = models.iterator(); each.hasNext();)
        {
            modelNames.add(((Model) each.next()).getName());
        }
        assertEquals(3, modelNames.size());
    }

}
