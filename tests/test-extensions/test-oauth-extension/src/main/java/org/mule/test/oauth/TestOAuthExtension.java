/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.values.extension.MyPojo;

@Extension(name = TestOAuthExtension.TEST_OAUTH_EXTENSION_NAME)
@Configurations({AuthCodeConfig.class, ClientCredentialsConfig.class, MixedConfig.class, WithPooledProviderConfig.class,
    WithValidationThatNeedsRefreshConfig.class})
@Xml(prefix = "test-oauth")
@Import(type = MyPojo.class)
public class TestOAuthExtension {

  public static final String TEST_OAUTH_EXTENSION_NAME = "Test OAuth Extension";

}
