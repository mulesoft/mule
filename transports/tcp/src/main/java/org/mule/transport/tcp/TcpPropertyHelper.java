/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import java.lang.Boolean;
import java.lang.String;

/**
 * Helper class to read properties for the TCP transport.
 */
public class TcpPropertyHelper
{
    public static final String MULE_TCP_BIND_LOCALHOST_TO_ALL_LOCAL_INTERFACES_PROPERTY = "mule.tcp.bindlocalhosttoalllocalinterfaces";

    /**
     * Returns whether localhost should be bound to all local interfaces or not.
     *
     * @return True if localhost should be bound to all local interfaces, false otherwise.
     */
    public static boolean isBindingLocalhostToAllLocalInterfaces()
    {
        return Boolean.valueOf(System.getProperty(MULE_TCP_BIND_LOCALHOST_TO_ALL_LOCAL_INTERFACES_PROPERTY, "false"));
    }

}
