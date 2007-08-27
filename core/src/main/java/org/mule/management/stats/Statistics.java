/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.stats;

import java.io.Serializable;

/**
 * <code>Statistics</code> TODO
 * 
 */
public interface Statistics extends Serializable
{
    /**
     * Are statistics logged
     */
    boolean isEnabled();

    /**
     * Enable statistics logs (this is a dynamic parameter)
     */
    void setEnabled(boolean b);

    void clear();

    void logSummary();
}
