/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

// @ThreadSafe
public class WaitableBoolean extends SynchronizedVariable
{
    // @GuardedBy(_lock)
    private boolean _value;

    public WaitableBoolean(boolean initialValue)
    {
        super();
        synchronized (_lock)
        {
            _value = initialValue;
        }
    }

    public WaitableBoolean(boolean initialValue, Object lock)
    {
        super(lock);
        synchronized (_lock)
        {
            _value = initialValue;
        }
    }

    public int compareTo(boolean other)
    {
        synchronized (_lock)
        {
            return (_value == other ? 0 : (_value ? 1 : -1));
        }
    }

    public int compareTo(WaitableBoolean other)
    {
        return this.compareTo(other.get());
    }

    public int compareTo(Object other)
    {
        return this.compareTo((WaitableBoolean)other);
    }

    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        else if (other instanceof WaitableBoolean)
        {
            synchronized (_lock)
            {
                return (_value == ((WaitableBoolean)other).get());
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
        synchronized (_lock)
        {
            return _value;
        }
    }

    public boolean set(boolean newValue)
    {
        synchronized (_lock)
        {
            _lock.notifyAll();
            boolean oldValue = _value;
            _value = newValue;
            return oldValue;
        }
    }

    public boolean compareAndSet(boolean assumedValue, boolean newValue)
    {
        synchronized (_lock)
        {
            boolean success = (_value == assumedValue);

            if (success)
            {
                _value = newValue;
                _lock.notifyAll();
            }

            return success;
        }
    }

    public boolean complement()
    {
        synchronized (_lock)
        {
            _lock.notifyAll();
            return (_value = !_value);
        }
    }

    public boolean and(boolean b)
    {
        synchronized (_lock)
        {
            _lock.notifyAll();
            return (_value &= b);
        }
    }

    public synchronized boolean or(boolean b)
    {
        synchronized (_lock)
        {
            _lock.notifyAll();
            return (_value |= b);
        }
    }

    public boolean xor(boolean b)
    {
        synchronized (_lock)
        {
            _lock.notifyAll();
            return (_value ^= b);
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
        synchronized (_lock)
        {
            while (_value != condition)
            {
                _lock.wait();
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
