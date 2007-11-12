/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.UMOTransformer;

import java.util.ArrayList;
import java.util.List;

public class ObjectToStringWithCollectionTestCase extends AbstractTransformerTestCase
{

    public UMOTransformer getTransformer() throws Exception
    {
        return new ObjectToString();
    }

    public Object getTestData()
    {
        List list = new ArrayList();
        list.add("one");
        list.add(null);
        list.add("three");
        return list;
    }

    public Object getResultData()
    {
        return "[one, null, three]";
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        // we do not want round trip transforming tested
        return null;
    }

}


