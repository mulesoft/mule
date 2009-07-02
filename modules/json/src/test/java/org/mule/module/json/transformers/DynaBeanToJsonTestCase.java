/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.json.transformers;

import org.mule.module.json.TestBean;
import org.mule.tck.AbstractMuleTestCase;

public class DynaBeanToJsonTestCase extends AbstractMuleTestCase
{

    private final String JSON_RESULT = "{\"doublev\":2.2,\"func1\":function(i){ return i; },\"id\":23,\"name\":\"json\",\"options\":[]}";
    ObjectToJson transformer;

    protected void doSetUp() throws Exception
    {
        transformer = new ObjectToJson();
    }

    public void testTransform() throws Exception
    {
        transformer.setReturnClass(String.class);
        transformer.setMuleContext(muleContext);
        TestBean bean = new TestBean("json", 23, 2.2, "function(i){ return i; }");
        String trasfRes = (String) transformer.transform(bean);
        assertEquals(JSON_RESULT, trasfRes);
    }
}
