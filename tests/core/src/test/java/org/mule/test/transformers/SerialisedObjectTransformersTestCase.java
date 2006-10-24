/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers;

import org.apache.commons.lang.SerializationUtils;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SerialisedObjectTransformersTestCase extends AbstractTransformerTestCase
{
    private Orange testObject = new Orange(new Integer(4), new Double(14.3), "nice!");

    public UMOTransformer getTransformer() throws Exception
    {
        return new SerializableToByteArray();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new ByteArrayToSerializable();
    }

    public Object getTestData()
    {
        return testObject;
    }

    public Object getResultData()
    {
        return SerializationUtils.serialize(testObject);

    }

}
