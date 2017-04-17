/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.extension.validation.api.CustomValidatorFactory;
import org.mule.extension.validation.api.ValidationExtension;
import org.mule.extension.validation.api.ValidationOptions;
import org.mule.extension.validation.api.Validator;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Defines a stateful operation of {@link ValidationExtension} which is capable of executing custom validators provided by a third
 * party. The {@link Validator} can be provided via a {@link CustomValidatorFactory} which means that the user could have specified either a
 * classname or a named reference to it.
 * <p/>
 * If the user provided a classname, then the {@link Class} that it represents is expected to have a default public constructor
 * which can be used to instantiate it.
 * <p/>
 * If the {@link Validator} is provided via a reference, then a lookup to the {@link MuleRegistry} will be performed.
 * <p/>
 * In either case, the referenced {@link Validator} is expected to be reusable and thread-safe. If you used a reference, then that
 * reference will most likely always point to the same instance. If you use a class, then an instance will be created and reused.
 *
 * @since 3.7.0
 */
public final class CustomValidatorOperation extends ValidationSupport {

  private final LoadingCache<CustomValidatorFactory, Validator> validatorCache =
      CacheBuilder.newBuilder().build(new CacheLoader<CustomValidatorFactory, Validator>() {

        @Override
        public Validator load(CustomValidatorFactory validatorSource) throws Exception {
          return validatorSource.getObject();
        }
      });

  public void customValidator(@Placement(order = 0) @ParameterGroup(name = "Validator") CustomValidatorFactory source,
                              @Placement(order = 1) @ParameterGroup(name = ERROR_GROUP) ValidationOptions options,
                              @Config ValidationExtension config)
      throws Exception {
    source.setMuleContext(config.getMuleContext());
    Validator validator = validatorCache.getUnchecked(source);
    validateWith(validator, createContext(options, config));
  }

  @Override
  protected void logSuccessfulValidation(Validator validator) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Successfully executed custom validator of type {}", validator.getClass().getName());
    }
  }
}
