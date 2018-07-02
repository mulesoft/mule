/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import static org.mule.api.config.MuleProperties.MULE_XML_RESET_CONTROLLER_AFTER_EACH_TRANSFORMATION;
import org.junit.Rule;
import org.mule.tck.junit4.rule.SystemProperty;

public class XsltResultDocumentTestCaseWithoutControllerReset extends XsltResultDocumentTestCase
{
    @Rule
    public SystemProperty useConnectorToRetrieveWsdl = new SystemProperty(MULE_XML_RESET_CONTROLLER_AFTER_EACH_TRANSFORMATION, "false");
    
}
