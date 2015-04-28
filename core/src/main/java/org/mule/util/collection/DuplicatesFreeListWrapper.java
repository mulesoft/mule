/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.collection;

import static org.mule.util.Preconditions.checkArgument;

import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.ForwardingListIterator;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A wrapper around a {@link #delegate} {@link List}
 * which makes sure that mutations of the list do not introduce
 * duplicated items. Duplicates already existing on the
 * wrapped list are allowed. Duplicates are silently rejected
 * without throwing any kind of exceptions.
 * <p/>
 * The actual behavior of this list depends on the behaviour of the
 * {@link #delegate}.
 * <p/>
 * Calling {@link #subList(int, int)} will also return an
 * instance of {@link DuplicatesFreeListWrapper} wrapping
 * the sublist returned by the {@link #delegate}.
 * <p/>
 * The {@link #iterator()}, {@link #listIterator()} and
 * {@link #listIterator(int)} methods also return
 * wrapped instances so that invokations to
 * {@link Iterator#remove()} are intercepted so that
 * the removed item can be re-added again without being
 * filtered
 *
 * @since 3.7.0
 */
public class DuplicatesFreeListWrapper<T> implements List<T>
{

    private Set<T> duplicatesHash = Sets.newConcurrentHashSet();
    private final List<T> delegate;

    public DuplicatesFreeListWrapper(List<T> delegate)
    {
        checkArgument(delegate != null, "delegate cannot be null");
        this.delegate = delegate;
        duplicatesHash.addAll(delegate);
    }

    @Override
    public int size()
    {
        return delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return duplicatesHash.contains(o);
    }

    @Override
    public Iterator<T> iterator()
    {
        return new IteratorWrapper<>(delegate.iterator());
    }

    @Override
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a)
    {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(T t)
    {
        if (duplicatesHash.add(t))
        {
            return delegate.add(t);
        }

        return false;
    }

    @Override
    public boolean remove(Object o)
    {
        duplicatesHash.remove(o);
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection)
    {
        Set<T> withoutDuplicates = new LinkedHashSet<>(collection);
        duplicatesHash.addAll(withoutDuplicates);
        return delegate.addAll(withoutDuplicates);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> collection)
    {
        Set<T> withoutDuplicates = new LinkedHashSet<>(collection);
        duplicatesHash.addAll(withoutDuplicates);
        return delegate.addAll(index, withoutDuplicates);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        duplicatesHash.removeAll(c);
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        duplicatesHash.retainAll(c);
        return delegate.retainAll(c);
    }

    @Override
    public void clear()
    {
        duplicatesHash.clear();
        delegate.clear();
    }

    @Override
    public boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    @Override
    public T get(int index)
    {
        return delegate.get(index);
    }

    @Override
    public T set(int index, T element)
    {
        if (duplicatesHash.add(element))
        {
            T previous = delegate.set(index, element);
            duplicatesHash.remove(previous);
            return previous;
        }
        else
        {
            return element;
        }
    }

    @Override
    public void add(int index, T element)
    {
        if (duplicatesHash.add(element))
        {
            delegate.add(index, element);
        }
    }

    @Override
    public T remove(int index)
    {
        T element = delegate.remove(index);
        duplicatesHash.remove(element);

        return element;
    }

    @Override
    public int indexOf(Object o)
    {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator()
    {
        return new ListIteratorWrapper<>(delegate.listIterator());
    }

    @Override
    public ListIterator<T> listIterator(int index)
    {
        return new ListIteratorWrapper<>(delegate.listIterator(index));
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex)
    {
        return new DuplicatesFreeListWrapper<>(delegate.subList(fromIndex, toIndex));
    }

    private class ListIteratorWrapper<T> extends ForwardingListIterator<T>
    {

        private final ListIterator<T> delegate;
        private T lastReturnedItem = null;

        private ListIteratorWrapper(ListIterator<T> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        protected ListIterator<T> delegate()
        {
            return delegate;
        }

        @Override
        public T next()
        {
            return lastReturnedItem = delegate.next();
        }

        @Override
        public T previous()
        {
            return lastReturnedItem = delegate.previous();
        }

        @Override
        public void remove()
        {
            if (lastReturnedItem != null)
            {
                duplicatesHash.remove(lastReturnedItem);
            }
            super.remove();
        }
    }

    private class IteratorWrapper<T> extends ForwardingIterator<T>
    {

        private final Iterator<T> delegate;
        private T lastReturnedItem = null;

        private IteratorWrapper(Iterator<T> delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public T next()
        {
            return lastReturnedItem = super.next();
        }

        @Override
        protected Iterator<T> delegate()
        {
            return delegate;
        }

        @Override
        public void remove()
        {
            super.remove();
            duplicatesHash.remove(lastReturnedItem);
        }
    }
}
