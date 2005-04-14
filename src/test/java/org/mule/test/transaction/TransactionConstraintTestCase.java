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
 */
package org.mule.test.transaction;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.transaction.constraints.ConstraintFilter;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TransactionConstraintTestCase extends AbstractMuleTestCase
{
    public void testConstraintFilter() throws Exception
    {
        ConstraintFilter filter = new ConstraintFilter();
        assertTrue(!filter.accept(new String()));
        assertTrue(filter.accept(getTestEvent("test")));

        ConstraintFilter clone = (ConstraintFilter)filter.clone();
        assertNotNull(clone);
        assertNotSame(filter, clone);
    }
}
