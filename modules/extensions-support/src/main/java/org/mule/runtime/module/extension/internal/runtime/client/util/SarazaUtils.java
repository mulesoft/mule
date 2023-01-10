/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.util;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.util.Map;
import java.util.Optional;

public class SarazaUtils {

  public static Map<String, Object> evaluate(ResolverSet resolverSet,
                                             Optional<ConfigurationInstance> configurationInstance,
                                             CoreEvent event) {
    ValueResolvingContext.Builder ctxBuilder = ValueResolvingContext.builder(event);
    configurationInstance.ifPresent(ctxBuilder::withConfig);

    try (ValueResolvingContext ctx = ctxBuilder.build()) {
      return resolverSet.resolve(ctx).asMap();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception found while evaluating parameters:" + e.getMessage()), e);
    }
  }
}
