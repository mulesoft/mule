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
package org.mule.management.mbeans;

import java.util.Map;

/**
 * <code>RouterStatsMBean</code> TODO
 *
 * @author Guillaume Nodet
 * @version $Revision$
 */
public interface RouterStatsMBean {

    int getCaughtMessages();
    
    int getNotRouted();

    int getTotalReceived();

    int getTotalRouted();
    
    Map getRouted();
}
