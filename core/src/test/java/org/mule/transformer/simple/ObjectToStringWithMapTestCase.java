/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

import java.util.Map;
import java.util.TreeMap;

public class ObjectToStringWithMapTestCase extends AbstractTransformerTestCase
{

    public Transformer getTransformer() throws Exception
    {
        return new ObjectToString();
    }

    public Object getTestData()
    {
        // TreeMap guarantees the order of keys. This is important for creating a test result
        // that is guaranteed to be comparable to the output of getResultData.
        Map map = new TreeMap();
        map.put("existingValue", "VALUE");
        map.put("nonexistingValue", null);
        return map;
    }

    public Object getResultData()
    {
        return "{existingValue=VALUE, nonexistingValue=null}";
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        // we do not want round trip transforming tested
        return null;
    }

}


