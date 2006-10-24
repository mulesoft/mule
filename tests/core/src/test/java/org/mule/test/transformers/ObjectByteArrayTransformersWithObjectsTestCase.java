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

import org.mule.transformers.simple.ByteArrayToObject;
import org.mule.transformers.simple.ObjectToByteArray;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ObjectByteArrayTransformersWithObjectsTestCase extends SerialisedObjectTransformersTestCase
{
    public UMOTransformer getTransformer() throws Exception
    {
        return new ObjectToByteArray();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new ByteArrayToObject();
    }

}
