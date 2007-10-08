/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.Iterator;

public class UUIDTestCase extends AbstractMuleTestCase
{

    public void testUUID()
    {
        assertNotNull(UUID.getUUID());
        String id = UUID.getUUID();
        assertNotNull(id);
        assertFalse(id.equals(UUID.getUUID()));
    }

    public void testComparison()
    {
        // these UUIDs were generated from independent VM runs and are ordered correctly
        String[] orderedUUIDs = new String[]{"ffc57c80-728d-11dc-afa2-572fe1c68dfa",
            "ffd61e51-728d-11dc-afa2-572fe1c68dfa", "ffe47632-728d-11dc-afa2-572fe1c68dfa",
            "fff53f13-728d-11dc-afa2-572fe1c68dfa", "00036fe4-728e-11dc-afa2-572fe1c68dfa",
            "001438c5-728e-11dc-afa2-572fe1c68dfa", "00226996-728e-11dc-afa2-572fe1c68dfa",
            "0030c177-728e-11dc-afa2-572fe1c68dfa", "00418a58-728e-11dc-afa2-572fe1c68dfa",
            "1bf9612f-728f-11dc-afd7-e1fea3d47ba1", "2a9882c2-728f-11dc-a191-b18dec97a373",
            "37db22d3-728f-11dc-a8f7-a5a3223c1540", "3f0bcd8d-728f-11dc-805a-b508f6822042",
            "a520596e-7290-11dc-b0b3-27c0633afd94"};

        // the same set of UUIDS in randomized order
        String[] unorderedUUIDs = new String[]{"00226996-728e-11dc-afa2-572fe1c68dfa",
            "0030c177-728e-11dc-afa2-572fe1c68dfa", "ffc57c80-728d-11dc-afa2-572fe1c68dfa",
            "a520596e-7290-11dc-b0b3-27c0633afd94", "ffe47632-728d-11dc-afa2-572fe1c68dfa",
            "fff53f13-728d-11dc-afa2-572fe1c68dfa", "00418a58-728e-11dc-afa2-572fe1c68dfa",
            "2a9882c2-728f-11dc-a191-b18dec97a373", "1bf9612f-728f-11dc-afd7-e1fea3d47ba1",
            "37db22d3-728f-11dc-a8f7-a5a3223c1540", "00036fe4-728e-11dc-afa2-572fe1c68dfa",
            "001438c5-728e-11dc-afa2-572fe1c68dfa", "3f0bcd8d-728f-11dc-805a-b508f6822042",
            "ffd61e51-728d-11dc-afa2-572fe1c68dfa"};

        // test that the comparison works for correctly ordered elements
        Iterator i = Arrays.asList(orderedUUIDs).iterator();
        while (i.hasNext())
        {
            String first = (String) i.next();
            String second = (String) i.next();
            assertTrue(UUID.getComparator().compare(first, second) < 0);
        }

        // now sort the unordered ids and check with the reference set
        String[] ordered = (String[]) unorderedUUIDs.clone();
        Arrays.sort(ordered, UUID.getComparator());
        assertTrue(Arrays.equals(orderedUUIDs, ordered));
    }
}
