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
package org.mule.impl.space;

import org.mule.util.queue.MemoryPersistenceStrategy;

/**
 * An in-memory space that is not persistent.  Useful for testing or where the space information
 * is not mission critical.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class VMSpaceFactory  extends DefaultSpaceFactory {

    public VMSpaceFactory() {
        super();
    }
    
    public VMSpaceFactory(boolean enableMonitorEvents) {
        super(enableMonitorEvents);
        setPersistenceStrategy(new MemoryPersistenceStrategy());
    }

    public VMSpaceFactory(boolean enableMonitorEvents, int capacity) {
        super(enableMonitorEvents, capacity);
        setPersistenceStrategy(new MemoryPersistenceStrategy());
    }

    public VMSpaceFactory(boolean enableMonitorEvents, boolean enableCaching) {
        super(enableMonitorEvents);
        setPersistenceStrategy(new MemoryPersistenceStrategy());
        setEnableCaching(enableCaching);
    }

    public VMSpaceFactory(boolean enableMonitorEvents, int capacity, boolean enableCaching) {
        super(enableMonitorEvents, capacity);
        setPersistenceStrategy(new MemoryPersistenceStrategy());
        setEnableCaching(enableCaching);
    }
}