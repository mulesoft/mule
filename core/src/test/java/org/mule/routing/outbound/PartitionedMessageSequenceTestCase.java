/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

@SmallTest
public class PartitionedMessageSequenceTestCase
{

    @Test
    public void wrapCollectionMessageSequence()
    {
        Collection<String> group1 = new ArrayList<String>();
        group1.add("one");
        group1.add("two");
        group1.add("three");
        group1.add("four");

        Collection<String> group2 = new ArrayList<String>();
        group2.add("five");
        group2.add("six");
        group2.add("seven");

        Collection<String> base = new ArrayList<String>();
        base.addAll(group1);
        base.addAll(group2);

        CollectionMessageSequence<String> cms = new CollectionMessageSequence<String>(base);
        int groupSize = group1.size();
        PartitionedMessageSequence<String> pms = new PartitionedMessageSequence<String>(cms, groupSize);
        assertEquals(2, pms.size());

        Collection<String> batchItem = (Collection<String>) pms.next();
        assertEquals(groupSize, batchItem.size());
        assertTrue(batchItem.containsAll(group1));

        batchItem = (Collection<String>) pms.next();
        assertEquals(group2.size(), batchItem.size());
        assertTrue(batchItem.containsAll(group2));

        assertFalse(pms.hasNext());
    }
}


