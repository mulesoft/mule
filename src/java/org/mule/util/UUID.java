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
 *
 */
package org.mule.util;

import java.io.Serializable;

import org.doomdark.uuid.UUIDGenerator;

/**
 * <code>UUID</code> Generates a UUID using the doom dark JUG library
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UUID implements Serializable
{
    private static UUIDGenerator gen = UUIDGenerator.getInstance();
    String uuid = null;

    public UUID()
    {
        uuid = gen.generateTimeBasedUUID().toString();
    }

    public String getUUID()
    {
        return uuid;
    }
}
