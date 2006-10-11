/* 
 * $Id: TransactionCallback.java 2176 2006-06-04 22:16:21Z holger $
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
package org.mule.providers.jms;

import javax.jms.Message;

import org.mule.transaction.TransactionCallback;

/**
 * @author Guillaume Nodet
 * @version $Revision: 2176 $
 */
public abstract class MessageTransactionCallback implements TransactionCallback
{
	protected Message message;
	
	public MessageTransactionCallback(Message message)
	{
		this.message=message;
	}
}
