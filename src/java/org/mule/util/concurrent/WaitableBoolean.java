/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Holger Hoffstaette
 */

public class WaitableBoolean extends SynchronizedVariable
{
	private AtomicBoolean _value;

	public WaitableBoolean(boolean initialValue)
	{
		super();
		_value = new AtomicBoolean(initialValue);
	}

	public WaitableBoolean(boolean initialValue, Object lock)
	{
		super(lock);
		_value = new AtomicBoolean(initialValue);
	}

	public int compareTo(boolean other)
	{
		boolean val = this.get();
		return (val == other) ? 0 : (val) ? 1 : -1;
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
			return get() == ((WaitableBoolean)other).get();
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
		return _value.toString();
	}

	public boolean get()
	{
		synchronized (_lock)
		{
			return _value.get();
		}
	}

	public boolean set(boolean newValue)
	{
		synchronized (_lock)
		{
			_lock.notifyAll();
			return _value.getAndSet(newValue);
		}
	}

	public boolean commit(boolean assumedValue, boolean newValue)
	{
		synchronized (_lock)
		{
			boolean success = _value.compareAndSet(assumedValue, newValue);
			if (success)
			{
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
			return !_value.get();
		}
	}

	public boolean and(boolean b)
	{
		synchronized (_lock)
		{
			_lock.notifyAll();
			return (_value.get() & b);
		}
	}

	public synchronized boolean or(boolean b)
	{
		synchronized (_lock)
		{
			_lock.notifyAll();
			return (_value.get() | b);
		}
	}

	public boolean xor(boolean b)
	{
		synchronized (_lock)
		{
			_lock.notifyAll();
			return (_value.get() ^ b);
		}
	}

	public void whenTrue(Runnable action) throws InterruptedException
	{
		this.whenEqual(true, action);
	}

	public void whenFalse(Runnable action) throws InterruptedException
	{
		this.whenEqual(false, action);
	}

	public void whenEqual(boolean condition, Runnable action) throws InterruptedException
	{
		synchronized (_lock)
		{
			while (!(_value.get() == condition))
			{
				_value.wait();
			}

			if (action != null)
			{
				action.run();
			}
		}
	}

	public void whenNotEqual(boolean condition, Runnable action) throws InterruptedException
	{
		this.whenEqual(!condition, action);
	}

}
