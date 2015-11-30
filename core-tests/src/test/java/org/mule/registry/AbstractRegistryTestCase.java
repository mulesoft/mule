/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public abstract class AbstractRegistryTestCase extends AbstractMuleTestCase
{

    public abstract Registry getRegistry();

    @Test
    public void testNotFoundCalls() throws RegistrationException
    {
        Registry r = getRegistry();
        Map<String, IOException> map = r.lookupByType(IOException.class);
        assertNotNull(map);
        assertEquals(0, map.size());

        IOException object = r.lookupObject(IOException.class);
        assertNull(object);

        object = r.lookupObject("foooooo");
        assertNull(object);

        Collection<IOException> list = r.lookupObjects(IOException.class);
        assertNotNull(list);
        assertEquals(0, list.size());
    }
}
