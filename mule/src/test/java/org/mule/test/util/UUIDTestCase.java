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

package org.mule.test.util;

import junit.framework.TestCase;

import org.mule.util.UUID;

/**
 * <code>UUIDTestCase</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UUIDTestCase extends TestCase
{

    /**
     * 
     */
    public UUIDTestCase()
    {
        super();
    }

    public void testUUID()
    {
        UUID uuid = new UUID();
        assertNotNull(uuid.getUUID());
        String id = uuid.getUUID().toString();
        assertNotNull(id);
        uuid = new UUID();
        assertTrue(!id.equals(uuid.getUUID().toString()));
    }

}
