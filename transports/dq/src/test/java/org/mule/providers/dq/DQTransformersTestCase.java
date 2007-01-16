/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.dq;

import org.mule.providers.dq.transformers.DQMessageToXml;
import org.mule.providers.dq.transformers.XmlToDQMessage;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.UMOTransformer;

public class DQTransformersTestCase extends AbstractTransformerTestCase
{
    public UMOTransformer getTransformer() throws Exception
    {
        return new DQMessageToXml();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new XmlToDQMessage();
    }

    public Object getTestData()
    {
        DQMessage msg = new DQMessage();
        msg.addEntry("entry1", "12345678910111");
        msg.addEntry("entry2", "yahooo");
        return msg;
    }

    public Object getResultData()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<DQMessage><entry name=\"entry1\">12345678910111</entry><entry name=\"entry2\">yahooo</entry></DQMessage>";
    }
}
