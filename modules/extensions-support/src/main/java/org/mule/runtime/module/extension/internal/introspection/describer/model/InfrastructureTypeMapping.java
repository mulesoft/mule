/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.THREADING_PROFILE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.config.ThreadingProfile;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Mapping for types considered of "Infrastructure", of the {@link Class} of the infrastructure type and the {@link String} name
 * of it.
 *
 * @since 4.0
 */
public class InfrastructureTypeMapping {

  private static Map<Class<?>, String> mapping = ImmutableMap.<Class<?>, String>builder()
      .put(TlsContextFactory.class, TLS_ATTRIBUTE_NAME).put(ThreadingProfile.class, THREADING_PROFILE_ATTRIBUTE_NAME).build();

  public static Map<Class<?>, String> getMap() {
    return mapping;
  }
}
