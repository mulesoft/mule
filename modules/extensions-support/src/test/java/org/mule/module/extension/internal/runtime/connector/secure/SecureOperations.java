/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.secure;

import org.mule.extension.api.annotation.param.display.Password;
import org.mule.extension.api.annotation.param.display.Text;

public class SecureOperations
{

    public String dummyOperation(@Password String secureParam, @Text String longText)
    {
        return secureParam;
    }
}
