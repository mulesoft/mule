/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.transaction.xa.queue;

import org.mule.transaction.xa.ResourceManagerException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public interface QueueSession {

	public Queue getQueue(String name);
	
	public void begin() throws ResourceManagerException;
	
	public void commit() throws ResourceManagerException;
	
	public void rollback() throws ResourceManagerException;
	
}
