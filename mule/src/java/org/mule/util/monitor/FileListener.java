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
package org.mule.util.monitor;

import java.io.File;
import java.io.IOException;

/**
 * Interface for listening to disk file changes.
 * 
 * @see FileMonitor
 * 
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 */
public interface FileListener
{
    /**
     * Called when one of the monitored files are created, deleted or modified.
     * 
     * @param file File which has been changed.
     */
    void fileChanged(File file) throws IOException;
}
