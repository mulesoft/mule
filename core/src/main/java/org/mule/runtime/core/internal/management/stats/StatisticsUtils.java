/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

public class StatisticsUtils {

  /**
   * Wraps the source as a visitable.
   * 
   * @param source to wrap.
   * @return a visitable wrapper of source. Empty if not a visitable class.
   */
  public static Optional<Visitable> visitable(Object source) {
    if (source instanceof InputStream) {
      return of(new VisitableInputStream((InputStream) source));
    }

    if (source instanceof Iterator) {
      return of(new VisitableIterator((Iterator) source));
    }

    if (source instanceof List) {
      return of(new VisitableList((List) source));
    }

    if (source instanceof Collection) {
      return of(new VisitableCollection((Collection) source));
    }

    if (source instanceof CursorStreamProvider) {
      return of(new VisitableCursorStreamProvider((CursorStreamProvider) source));
    }

    return empty();
  }

}
