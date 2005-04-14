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
package org.mule.util.queue;

import EDU.oswego.cs.dl.util.concurrent.BoundedChannel;
import org.mule.umo.UMOEvent;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>PersistenceStrategy</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface PersistenceStrategy
{
    public void store(UMOEvent event) throws PersistentQueueException;

    public boolean remove(UMOEvent event) throws PersistentQueueException;

    public void initialise(BoundedChannel queue, String componentName) throws InitialisationException;

    public void dispose() throws PersistentQueueException;

}
