/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap;

/**
 * <code>WsdlServiceFinder</code> finds a the connector service to use by checking
 * the classpath for jars required for each of the soap connector implementations
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class WsdlServiceFinder extends SoapServiceFinder
{
    private static final String PROTOCOL_PREFIX = "wsdl-";

    protected String getProtocolFromKey(String key)
    {
        return PROTOCOL_PREFIX + super.getProtocolFromKey(key);
    }

}
