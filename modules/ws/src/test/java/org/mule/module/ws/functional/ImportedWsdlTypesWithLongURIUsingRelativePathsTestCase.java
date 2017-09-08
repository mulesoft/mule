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
 * Test that verifies a WSDL that involves an import that uses a very long URI with relative paths.
 * This is also the scenario where a lot of XSD's imports each other using relative paths.
 * The URI to verify keeps getting longer until an exception is raised because the length of the file
 * is too long. 
 */
public class ImportedWsdlTypesWithLongURIUsingRelativePathsTestCase extends ImportedWsdlTypesFunctionalTestCase
{
    @Rule
    public SystemProperty wsdlLocation = new SystemProperty("wsdlLocation", "TestImportedTypesWithLongURIUsingRelativePaths.wsdl");
}
