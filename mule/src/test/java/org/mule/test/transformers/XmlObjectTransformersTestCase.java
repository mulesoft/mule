/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToObject;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmlObjectTransformersTestCase extends AbstractTransformerTestCase
{
    private Apple testObject = null;

    public XmlObjectTransformersTestCase() {
        testObject = new Apple();
        testObject.wash();
    }

    public UMOTransformer getTransformer() throws Exception
    {
        return new ObjectToXml();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new XmlToObject();
    }

    public Object getTestData()
    {
        return testObject;
    }

    public Object getResultData()
    {
        return "<org.mule.tck.testmodels.fruit.Apple>\n" +
                "  <bitten>false</bitten>\n" +
                "  <washed>true</washed>\n" +
                "</org.mule.tck.testmodels.fruit.Apple>";
    }
}
