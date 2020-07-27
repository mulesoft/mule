/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tooling.extensions.metadata.internal.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.tooling.extensions.metadata.internal.operation.SimpleOperations;
import org.mule.tooling.extensions.metadata.internal.source.SimpleSource;

@Operations({SimpleOperations.class})
@Sources({SimpleSource.class})
@Configuration(name="config")
public class SimpleConfiguration {

}
