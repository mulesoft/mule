/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

/**
 * Loads the {@link ClassLoaderConfiguration} for Mule artifacts
 * <p/>
 * Explicitly defined to enable definition of implementations using SPI.
 *
 * @since 4.5
 */
public interface ClassLoaderConfigurationLoader extends DescriptorLoader<ClassLoaderConfiguration> {
}
