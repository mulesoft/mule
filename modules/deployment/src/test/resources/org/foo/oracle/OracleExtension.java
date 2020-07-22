/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.oracle;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.foo.oracle.OracleOperation;

import javax.inject.Inject;

/**
 * Extension for testing purposes
 */
@Extension(name = "Oracle")
@Operations({OracleOperation.class})
public class OracleExtension {

    public OracleExtension() {}

    public String getMessage() {
        return "Let's connect to oracledb!!";
    }
}
