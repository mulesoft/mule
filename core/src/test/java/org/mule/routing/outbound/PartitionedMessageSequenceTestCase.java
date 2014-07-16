/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        Collection<String> batchItem = pms.next();
        assertEquals(groupSize, batchItem.size());
        assertTrue(batchItem.containsAll(group1));

        batchItem = pms.next();
        assertEquals(group2.size(), batchItem.size());
        assertTrue(batchItem.containsAll(group2));

        assertFalse(pms.hasNext());
    }
}


