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
 */
package org.mule.impl.internal.events;

import org.mule.umo.manager.UMOServerEventListener;

/**
 * <code>CustomEventListener</code> is an observer interface that can be used to
 * listen for Custom events using <code>UMOManager.fireCustomEvent(..)</code>.
 * Custom events can be used by components and other objects such as routers,
 * transformers, agents, etc to communicate a change of state to each other.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface CustomEventListener extends UMOServerEventListener
{
}
