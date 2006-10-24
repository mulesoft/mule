/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import java.util.Map;

/**
 * <code>RouterStatsMBean</code> TODO
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 */
public interface RouterStatsMBean
{

    long getCaughtMessages();

    long getNotRouted();

    long getTotalReceived();

    long getTotalRouted();

    Map getRouted();
}
