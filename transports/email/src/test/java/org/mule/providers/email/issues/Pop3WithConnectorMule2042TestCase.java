/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.issues;

import org.mule.providers.email.functional.AbstractEmailFunctionalTestCase;

public class Pop3WithConnectorMule2042TestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3WithConnectorMule2042TestCase()
    {
        super(65435, STRING_MESSAGE, "pop3", "pop3-with-connector-mule-2042-test.xml");
    }

    public void testRequest() throws Exception
    {
        doRequest();
    }

}