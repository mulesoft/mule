/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

@Extension(name = "Test OAuth Extension")
@Configurations({AuthCodeConfig.class, ClientCredentialsConfig.class, MixedConfig.class, WithPooledProviderConfig.class})
@Xml(prefix = "test-oauth")
public class TestOAuthExtension {


}
