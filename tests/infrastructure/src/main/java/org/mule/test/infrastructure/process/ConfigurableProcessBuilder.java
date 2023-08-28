/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure.process;

public interface ConfigurableProcessBuilder {

  ConfigurableProcessBuilder addConfigurationAttribute(String propertyName, String propertyValue);

}
