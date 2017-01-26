/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.StringUtils.isBlank;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * A factory object for providing instances by either referencing their classname (through the {@link #type} attribute, or a
 * {@link MuleRegistry} reference (through the {@link #ref} one.
 * <p>
 * When the {@link #type} attribute is used to reference a type, then a new instance is returned each time that
 * {@link CustomValidatorFactory#getObject()} is invoked. That type is also expected to have a public default {@link Constructor}.
 * <p>
 * When a {@link #ref} is provided, then that value is searched by using the {@link MuleRegistry#get(String)}. Notice however that
 * the reference will be re fetched each time that {@link CustomValidatorFactory#getObject()} is invoked
 * <p>
 * The {@link #type} and {@link #ref} attributes are mutually exclusive. A {@link IllegalArgumentException} is thrown if both are
 * set by the time {@link CustomValidatorFactory#getObject()} is invoked. The same exception is also thrown if none of them are.
 * <p>
 * Instances of this class are to be considered thread-safe and reusable.
 *
 * @since 4.0
 */
@ExclusiveOptionals(isOneRequired = true)
public final class CustomValidatorFactory {

  private MuleContext muleContext;

  @Parameter
  @Alias("class")
  @Optional
  private String type;

  @Parameter
  @Optional
  @XmlHints(allowInlineDefinition = false)
  private Validator ref;

  public CustomValidatorFactory() {}

  public CustomValidatorFactory(String type, Validator ref) {
    this.type = type;
    this.ref = ref;
  }

  public final Validator getObject() {
    return !isBlank(type) ? doGetByClassName(muleContext) : ref;
  }

  protected Validator doGetByClassName(MuleContext muleContext) {
    checkArgument(muleContext != null, "Mule Context is required for loading a Validator class");
    Class<Validator> objectClass;

    try {
      objectClass = (Class<Validator>) ClassUtils.loadClass(type, getClass());
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(format("Could not find class %s", type), e);
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(format("Could %s is not a %s", type, Validator.class.getName()), e);
    }

    try {
      Validator validator = objectClass.newInstance();
      muleContext.getRegistry().applyProcessors(validator);
      return validator;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + type), e);
    }
  }

  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public Validator getRef() {
    return ref;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof CustomValidatorFactory) {
      CustomValidatorFactory that = (CustomValidatorFactory) other;
      return Objects.equals(type, that.type) && Objects.equals(ref, that.ref);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, ref);
  }
}
