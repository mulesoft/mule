/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security.provider;

/**
 * A holder for some JDK-level SSL/TLS properties.
 */
public interface SecurityProviderInfo
{

    String getKeyManagerAlgorithm();

    String getProtocolHandler();

    String getProviderClass();

}
