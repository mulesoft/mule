/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServiceNotificationListener;
import org.mule.api.context.notification.ManagerNotificationListener;
import org.mule.api.context.notification.ModelNotificationListener;

import java.util.List;
import java.util.LinkedList;

public class ServerNotificationLogger
        extends AbstractNotificationLogger
        implements ServiceNotificationListener, ManagerNotificationListener, ModelNotificationListener
{

}
