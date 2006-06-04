/* 
 * $Id$
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

import org.mule.util.UUID;

import junit.framework.TestCase;

/**
 * <code>UUIDTestCase</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UUIDTestCase extends TestCase
{

    public UUIDTestCase()
    {
        super();
    }

    public void testUUID()
    {
        assertNotNull(UUID.getUUID());
        String id = UUID.getUUID();
        assertNotNull(id);
        assertTrue(!id.equals(UUID.getUUID()));
    }

}
