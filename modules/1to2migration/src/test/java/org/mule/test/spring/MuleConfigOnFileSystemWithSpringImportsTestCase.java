/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

/**
 * Tests a Mule config from the file system (i.e., not from the classpath) which loads Spring configs 
 * via <code>&lt;import resource="file.xml"/&gt;</code> statements.  The Spring configs will be loaded from the same directory
 * as the Mule config (Spring uses relative path by default).
 */
public class MuleConfigOnFileSystemWithSpringImportsTestCase extends MuleConfigWithSpringImportsTestCase
{
    public String getConfigResources()
    {
        // TODO TC this is guaranteed to fail, when e.g. running in a reactor build fromt the root
        // update: it work on CI, but fails for a single run in IDE, maybe getting all resources
        // from classpath and copying them to the target folder for the test would be more robust?
        return "file:./src/test/resources/mule-config-with-imports.xml";
    }
}


