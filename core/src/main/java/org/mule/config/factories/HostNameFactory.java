/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.factories;

import org.mule.config.PropertyFactory;

import java.net.InetAddress;
import java.util.Map;

/**
 * Extracts the local hostname from the local system
 */
public class HostNameFactory implements PropertyFactory
{

    public Object create(Map props) throws Exception
    {
        return InetAddress.getLocalHost().getHostName();
    }

}
