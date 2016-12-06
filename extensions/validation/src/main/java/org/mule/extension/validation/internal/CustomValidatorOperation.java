/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.mule.extension.validation.api.ObjectSource;
import org.mule.extension.validation.api.ValidationExtension;
import org.mule.extension.validation.api.ValidationOptions;
import org.mule.extension.validation.api.Validator;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Defines a stateful operation of {@link ValidationExtension} which is capable of executing custom validators provided by a third
 * party. The {@link Validator} can be provided via a {@link ObjectSource} which means that the user could have specified either a
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

  private final LoadingCache<ValidatorSource, Validator> class2ValidatorCache =
      CacheBuilder.newBuilder().build(new CacheLoader<ValidatorSource, Validator>() {

        @Override
        public Validator load(ValidatorSource validatorSource) throws Exception {
          return validatorSource.createValidator();
        }
      });

  public void customValidator(@Placement(order = 0) @ParameterGroup("Validator") ObjectSource<Validator> source,
                              @Placement(order = 1) @ParameterGroup(ERROR_GROUP) ValidationOptions options,
                              Event event,
                              @UseConfig ValidationExtension config)
      throws Exception {
    ValidatorSource validatorSource = new ValidatorSource(source.getType(), source.getRef());
    Validator validator = validatorSource.getObject(muleContext);

    validateWith(validator, createContext(options, event, config), event);
  }

  @Override
  protected void logSuccessfulValidation(Validator validator, Event event) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Successfully executed custom validator of type {} on message: {}", validator.getClass().getName(),
                   event.getMessage());
    }
  }

  private class ValidatorSource extends ObjectSource<Validator> {

    public ValidatorSource(String type, String ref) {
      super(type, ref);
    }

    @Override
    protected Validator doGetByClassName() {
      return class2ValidatorCache.getUnchecked(this);
    }

    private Validator createValidator() {
      return super.doGetByClassName();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ValidatorSource) {
        return getType().equals(((ValidatorSource) obj).getType());
      }

      return false;
    }

    @Override
    public int hashCode() {
      return getType().hashCode();
    }
  }
}
