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
 */
package org.mule.test.usecases.sync;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class TcpReplyComponent implements Callable {

	/* (non-Javadoc)
	 * @see org.mule.umo.lifecycle.Callable#onCall(org.mule.umo.UMOEventContext)
	 */
	public Object onCall(UMOEventContext eventContext) throws Exception {
		return eventContext.getMessageAsString() + " response";
	}

}
