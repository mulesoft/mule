/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

/**
 * Test that verifies a WSDL that involves an XSD definition with nested included XSD's.
 */
public class IncludedXsdTypesWithSingleLevelFunctionalTestCase extends IncludedXsdTypesFunctionalTestCase
{
    @Rule
    public SystemProperty wsdlLocation = new SystemProperty("wsdlLocation", "TestIncludedTypes.wsdl");
}
