/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
