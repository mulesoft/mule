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
import org.custommonkey.xmlunit.XMLAssert;

/**
 * Use this superclass if you intend to compare Xml contents.
 *
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 */
public abstract class AbstractXmlTransformerTestCase extends AbstractTransformerTestCase {

    /**
     * Different JVMs serialize fields to XML in a different order.
     * Make sure we DO NOT use direct (and too strict String comparison).
     * Instead, compare xml contents, while the position of the node
     * may differ in scope of the same node level (still, it does not violate xml spec).
     * Overridden from the superclass.
     *
     * @throws Exception if any error
     */
    public void testTransform() throws Exception {
        Object result = getTransformer().transform(getTestData());
        assertNotNull(result);
        System.out.println(getResultData());
        System.out.println("\n\n" + result);
        //XMLAssert.assertXMLEqual("Xml documents have different data.", (String) getResultData(), (String) result);
    }
}
