/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class UUIDTestCase extends AbstractMuleTestCase
{

    @Test
    public void testGenerateUniqueAndIncrementalIds() throws Exception
    {
        final Set<String> ids = new HashSet<String>();
        final List<Object[]> idsWithIndexes = new ArrayList<Object[]>(1000);
        final int numberOfIdsToGenerate = 10000;
        for (int index = 0; index < numberOfIdsToGenerate; index++)
        {
            String generatedId = UUID.getUUID();
            idsWithIndexes.add(new Object[]{generatedId, Integer.valueOf(index)});
            if (ids.contains(generatedId))
            {
                fail("REPEATED ID :" + index + ": " + generatedId);
            }
            else
            {
                ids.add(generatedId);
            }
        }
        final Comparator<Object[]> comparatorById = new Comparator<Object[]>()
        {
            public int compare(Object[] o1, Object[] o2)
            {
                return ((String) o1[0]).compareTo((String) o2[0]);
            }
        };
        Collections.sort(idsWithIndexes, comparatorById);
        for (int index = 0; index < numberOfIdsToGenerate; index++)
        {
            assertEquals(Integer.valueOf(index), idsWithIndexes.get(index)[1]);
        }
    }

}


