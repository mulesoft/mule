/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;

import org.mule.api.model.Model;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

public class DefaultModelNames extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "default-model-names.xml";
    }

    @Test
    public void testNames()
    {
        Collection<Model> models = muleContext.getRegistry().lookupObjects(Model.class);
        assertEquals(3, models.size()); // includes system model
        Set<String> modelNames = new HashSet<String>();
        for (Iterator each = models.iterator(); each.hasNext();)
        {
            modelNames.add(((Model) each.next()).getName());
        }
        assertEquals(3, modelNames.size());
    }
}
