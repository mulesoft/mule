/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.tck.AbstractMuleTestCase;

public class MuleEndpointURITestCase extends AbstractMuleTestCase
{

    public void testEquality() throws Exception
    {
        MuleEndpointURI u1 = new MuleEndpointURI("test://mule:secret@jabber.org:6666/ross@jabber.org");
        MuleEndpointURI u2 = new MuleEndpointURI("test://mule:secret@jabber.org:6666/ross@jabber.org");

        assertEquals(u1, u2);
        assertEquals(u2, u1);
        assertEquals(u1.hashCode(), u2.hashCode());
        assertEquals(u2.hashCode(), u1.hashCode());

        MuleEndpointURI u3 = new MuleEndpointURI(u1);
        assertEquals(u1, u3);
        assertEquals(u2, u3);
        assertEquals(u3, u1);
        assertEquals(u3, u2);
        assertEquals(u1.hashCode(), u3.hashCode());
        assertEquals(u2.hashCode(), u3.hashCode());
    }

}
