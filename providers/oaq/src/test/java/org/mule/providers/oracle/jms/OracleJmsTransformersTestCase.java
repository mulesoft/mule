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
 *
 */
package org.mule.providers.oracle.jms;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.providers.oracle.jms.transformers.StringToXMLMessage;
import org.mule.providers.oracle.jms.transformers.XMLMessageToString;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * 
 * TODO How can we unit test the <code>StringToXMLMessage</code> and 
 * <code>XMLMessageToString</code> transformers without a real AQjmsSession? 
 */
public class OracleJmsTransformersTestCase extends AbstractTransformerTestCase
{
    public UMOTransformer getTransformer() throws Exception {
        return new StringToXMLMessage();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception {
        return new XMLMessageToString();
    }

    public Object getTestData() {
        return "<msg attrib=\"attribute\">This is an XML message.</msg>";
    }

    /** TODO An AdtMessage cannot be created without an AQjmsSession. */
    public Object getResultData() {
    	return null/*AdtMessage*/;
    }
}
