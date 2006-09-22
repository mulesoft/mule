/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.providers.jms.transformers.AbstractJmsTransformer;
import org.mule.tck.AbstractMuleTestCase;

/**
 * TODO: document this class
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JmsTransformerTestCase extends AbstractMuleTestCase
{

    public void testHeaders()
    {
        assertEquals("identifier", AbstractJmsTransformer.encodeHeader("identifier"));
        assertEquals("ident_ifier", AbstractJmsTransformer.encodeHeader("ident_ifier"));
        assertEquals("ident_ifier", AbstractJmsTransformer.encodeHeader("ident-ifier"));
    }

}
