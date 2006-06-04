/*
 * $Id$
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

import org.mule.util.queue.FilePersistenceStrategy;

/**
 * A space that uses file-based persistence and enables caching by default
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FileSpaceFactory extends DefaultSpaceFactory {

    public FileSpaceFactory(boolean enableMonitorEvents) {
        super(enableMonitorEvents);
        setEnableCaching(true);
        setPersistenceStrategy(new FilePersistenceStrategy());
    }

    public FileSpaceFactory(boolean enableMonitorEvents, int capacity) {
        super(enableMonitorEvents, capacity);
        setEnableCaching(true);
        setPersistenceStrategy(new FilePersistenceStrategy());
    }
}
