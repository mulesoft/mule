/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

import java.util.ArrayList;
import java.util.List;

public class ObjectToStringWithCollectionTestCase extends AbstractTransformerTestCase
{
    @Override
    public Transformer getTransformer() throws Exception
    {
        return new ObjectToString();
    }

    @Override
    public Object getTestData()
    {
        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add(null);
        list.add("three");
        return list;
    }

    @Override
    public Object getResultData()
    {
        return "[one, null, three]";
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        // we do not want round trip transforming tested
        return null;
    }
}
