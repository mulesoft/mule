/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;


public class UUIDTestCase extends AbstractMuleTestCase {

  @Test
  public void testGenerateUniqueAndIncrementalIds() throws Exception {
    final Set<String> ids = new HashSet<String>();
    final List<Object[]> idsWithIndexes = new ArrayList<Object[]>(1000);
    final int numberOfIdsToGenerate = 10000;
    for (int index = 0; index < numberOfIdsToGenerate; index++) {
      String generatedId = UUID.getUUID();
      idsWithIndexes.add(new Object[] {generatedId, Integer.valueOf(index)});
      if (ids.contains(generatedId)) {
        fail("REPEATED ID :" + index + ": " + generatedId);
      } else {
        ids.add(generatedId);
      }
    }
    final Comparator<Object[]> comparatorById = new Comparator<Object[]>() {

      public int compare(Object[] o1, Object[] o2) {
        return ((String) o1[0]).compareTo((String) o2[0]);
      }
    };
    Collections.sort(idsWithIndexes, comparatorById);
    for (int index = 0; index < numberOfIdsToGenerate; index++) {
      assertEquals(Integer.valueOf(index), idsWithIndexes.get(index)[1]);
    }
  }

}


