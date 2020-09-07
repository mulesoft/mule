/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats.visitor;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

/**
 * Visitor that returns an instance as a result of an operation on the visited element.
 */
public interface Visitor<T> {

  /**
   * @param visitable a visitable inputstream
   * @return result of the inputstream which has been visited
   */
  InputStream visitInputStream(VisitableInputStream visitable);

  /**
   * @param visitable a visitable iterator
   * @return result of the iterator which has been visited
   */
  Iterator<T> visitIterator(VisitableIterator<T> visible);

  /**
   * @param visitable a visitable collection
   * @return result of the collection which has been visited
   */
  Collection<T> visitCollection(VisitableCollection<T> visitable);

  /**
   * @param visitable a visitable list
   * @return result of the list which has been visited
   */
  List<T> visitList(VisitableList<T> visitableList);

  /**
   * @param visitable a visitable set
   * @return result of the set which has been visited
   */
  Set<T> visitSet(VisitableSet<T> visitableSet);

  /**
   * @param visitable a visitable set
   * @return result of the cursor provider which has been visited
   */
  CursorStreamProvider visitCursorStreamProvider(VisitableCursorStreamProvider visitableCursorStreamProvider);
}
