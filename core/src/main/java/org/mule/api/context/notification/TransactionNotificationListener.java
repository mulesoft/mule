/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
/*
* $Id:TransactionNotificationListener.java 7275 2007-06-28 02:51:31Z aperepel $
* --------------------------------------------------------------------------------------
* Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
*
* The software in this package is published under the terms of the CPAL v1.0
* license, a copy of which has been included with this distribution in the
* LICENSE.txt file.
*/

package org.mule.api.context.notification;

import org.mule.context.notification.TransactionNotification;


/**
 * <code>TransactionNotificationListener</code> is an observer interface that objects
 * can implement and then register themselves with the Mule manager to be notified
 * when a Transaction event occurs.
 */
public interface TransactionNotificationListener<T extends TransactionNotification> extends ServerNotificationListener<TransactionNotification>
{
    // no methods
}
