/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
