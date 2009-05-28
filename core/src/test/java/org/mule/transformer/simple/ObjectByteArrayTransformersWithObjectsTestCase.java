/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;

public class ObjectByteArrayTransformersWithObjectsTestCase extends SerialisedObjectTransformersTestCase
{

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new ObjectToByteArray();
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return new ByteArrayToObject();
    }

}
