/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

// @ThreadSafe
public class WaitableBoolean extends SynchronizedVariable
{
    // @GuardedBy(lock)
    private boolean value;

    public WaitableBoolean(boolean initialValue)
    {
        super();
        synchronized (lock)
        {
            value = initialValue;
        }
    }

    public WaitableBoolean(boolean initialValue, Object lock)
    {
        super(lock);
        synchronized (this.lock)
        {
            value = initialValue;
        }
    }

    public int compareTo(boolean other)
    {
        synchronized (lock)
        {
            return (value == other ? 0 : (value ? 1 : -1));
        }
    }

    public int compareTo(WaitableBoolean other)
    {
        return this.compareTo(other.get());
    }

    public int compareTo(Object other)
    {
        return this.compareTo((WaitableBoolean) other);
    }

    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        else if (other instanceof WaitableBoolean)
        {
            synchronized (lock)
            {
                return (value == ((WaitableBoolean) other).get());
            }
        }
        else
        {
            return false;
        }
    }

    public int hashCode()
    {
        return (this.get() ? 3412688 : 8319343); // entirely arbitrary
    }

    public String toString()
    {
        return Boolean.toString(this.get());
    }

    public boolean get()
    {
        synchronized (lock)
        {
            return value;
        }
    }

    public boolean set(boolean newValue)
    {
        synchronized (lock)
        {
            lock.notifyAll();
            boolean oldValue = value;
            value = newValue;
            return oldValue;
        }
    }

    public boolean compareAndSet(boolean assumedValue, boolean newValue)
    {
        synchronized (lock)
        {
            boolean success = (value == assumedValue);

            if (success)
            {
                value = newValue;
                lock.notifyAll();
            }

            return success;
        }
    }

    public boolean complement()
    {
        synchronized (lock)
        {
            lock.notifyAll();
            return (value = !value);
        }
    }

    public boolean and(boolean b)
    {
        synchronized (lock)
        {
            lock.notifyAll();
            return (value &= b);
        }
    }

    public synchronized boolean or(boolean b)
    {
        synchronized (lock)
        {
            lock.notifyAll();
            return (value |= b);
        }
    }

    public boolean xor(boolean b)
    {
        synchronized (lock)
        {
            lock.notifyAll();
            return (value ^= b);
        }
    }

    public void whenTrue(Runnable action) throws InterruptedException
    {
        this.whenEqual(true, action);
    }

    public void whenFalse(Runnable action) throws InterruptedException
    {
        this.whenNotEqual(true, action);
    }

    public void whenEqual(boolean condition, Runnable action) throws InterruptedException
    {
        synchronized (lock)
        {
            while (value != condition)
            {
                lock.wait();
            }

            if (action != null)
            {
                this.execute(action);
            }
        }
    }

    public void whenNotEqual(boolean condition, Runnable action) throws InterruptedException
    {
        this.whenEqual(!condition, action);
    }

}
