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
package org.mule.impl.internal.events;

import org.mule.umo.manager.UMOServerEventListener;

/**
 * <code>ManagerEventListener</code> is an observer interface that objects can implement and then
 * register themselves with the Mule manager to be notified when a Manager event occurs.
 *
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface ManagerEventListener extends UMOServerEventListener
{
}
