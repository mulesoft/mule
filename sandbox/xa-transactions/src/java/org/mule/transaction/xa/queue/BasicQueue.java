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
package org.mule.transaction.xa.queue;

import java.util.LinkedList;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class BasicQueue implements Queue {

	private LinkedList list = new LinkedList();
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.Queue#put(java.lang.Object)
	 */
	synchronized public void put(Object o) {
		if (o == null) {
			throw new IllegalArgumentException("Can not insert null objects");
		}
		list.add(o);
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.Queue#take()
	 */
	synchronized public Object take() {
		int sz = size();
		if (sz > 0) {
			return list.remove(0);
		} else {
			return null;
		}
	}
	
	synchronized public int size() {
		return list.size();
	}
	
}