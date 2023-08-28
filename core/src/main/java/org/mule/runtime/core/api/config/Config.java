/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config;

/**
 * A generic component's configuration. For now it's just a marker to enable a lifecycle mechanism in which configs are
 * initialised and started before the owning component.
 *
 * @since 3.7.0
 */
public interface Config {

}
