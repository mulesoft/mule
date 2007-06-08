/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.functional;

public class Pop3StringFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3StringFunctionalTestCase()
    {
        super(65445, STRING_MESSAGE, "pop3", "pop3-string-functional-test.xml");
    }

    public void testReceive() throws Exception
    {
        doReceive();
    }

}