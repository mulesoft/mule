/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import org.mule.tck.size.SmallTest;

@SmallTest
public class FiledErrorDebugInfoTestCase extends AbstractFieldDebugInfoTestCase<Exception>
{

    @Override
    protected Exception getValue()
    {
        return new Exception();
    }

    @Override
    protected void createFieldDebugInfo(String name, Class type, Exception value)
    {
        new ErrorFieldDebugInfo(name, type, value);
    }
}
