/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Selects a preferred object from a collection of instances of the same type
 * based on the weight of the {@link Preferred} annotation. If there is no
 * preferred instances, then it just returns the first object in the collection.
 */
public class PreferredObjectSelector<T>
{

    /**
     * Selects a preferred object from instances returned by an {@link Iterator}.
     * <p/>
     * The preferred instance will be the instance annotated with {@link Preferred}
     * annotation with the highest weight attribute if there is any, or a non
     * annotated class otherwise.
     *
     * @param iterator contains the objects to select from
     * @return the preferred instance
     */
    public T select(Iterator<T> iterator)
    {
        List<T> candidates = new LinkedList<T>();

        while (iterator.hasNext())
        {
            candidates.add(iterator.next());
        }

        return select(candidates);
    }

    /**
     * Selects a preferred object from instances contained in a {@link List}.
     * <p/>
     * The preferred instance will be the instance annotated with {@link Preferred}
     * annotation with the highest weight attribute if there is any, or a non
     * annotated class otherwise.
     *
     * @param candidates contains the objects to select from
     * @return the preferred instance
     */
    public T select(List<T> candidates)
    {
        Collections.sort(candidates, new Comparator<T>()
        {
            private PreferredComparator preferredComparator = new PreferredComparator();

            public int compare(T threadPoolFactory, T threadPoolFactory1)
            {
                final Preferred preferred = threadPoolFactory.getClass().getAnnotation(Preferred.class);
                final Preferred preferred1 = threadPoolFactory1.getClass().getAnnotation(Preferred.class);

                return preferredComparator.compare(preferred, preferred1);
            }
        });

        return candidates.get(candidates.size() - 1);
    }
}
