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
 *
 */
package org.mule.util;

import org.doomdark.uuid.UUIDGenerator;

import java.io.Serializable;

/**
 * <code>UUID</code> Generates a UUID using the doom dark JUG library
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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
