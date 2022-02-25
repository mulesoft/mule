/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.Class.forName;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.IMITATE_SUPER_CLASS;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;

import static org.mule.runtime.core.internal.util.CompositeClassLoader.from;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectTypeProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;

/**
 * Repository for storing the dynamic class generated to mimic {@link org.springframework.beans.factory.FactoryBean} from an
 * {@link ObjectFactory}. This is done because we need dependency injection and instrospection done over the {@link ObjectFactory}
 * without the user knowing about it.
 * <p>
 * The created {@link org.springframework.beans.factory.FactoryBean} is the one that receives the injection of fields declared by
 * the {@link ObjectFactory}. It also provides information about the instance type that is creating since it's used to know the
 * order in which beans must be initialised based on the dependencies between them.
 *
 * @since 4.0
 */
public class ObjectFactoryClassRepository {

  /**
   * Retrieves a {@link Class} for the {@link ObjectFactory} defined by the {@code objectFactoryType} parameter. Once acquired the
   * {@code Class} instance should not be reused for another {@link ComponentBuildingDefinition}.
   *
   * @param objectFactoryType the {@link ObjectFactory} of the component
   * @return the {@code FactoryBean} class to be used by spring for the provided configuration.
   */
  public Class<ObjectFactory> getObjectFactoryClass(Class objectFactoryType) {
    synchronized (this.getClass().getClassLoader()) {
      String name = objectFactoryType.getName() + "_ByteBuddy";
      try {
        return (Class<ObjectFactory>) forName(name, true, this.getClass().getClassLoader());
      } catch (ClassNotFoundException e) {
        // class doesn't exist, generate
      }
      return createObjectFactoryDynamicClass(objectFactoryType, name);
    }
  }

  private Class<ObjectFactory> createObjectFactoryDynamicClass(Class objectFactoryType, String name) {
    final GenericInterceptor interceptor = new GenericInterceptor();
    return new ByteBuddy()
        .subclass(objectFactoryType, IMITATE_SUPER_CLASS)
        .name(name)
        .defineField("isSingleton", Boolean.class, PRIVATE)
        .defineField("objectTypeClass", Class.class, PRIVATE)
        .defineField("isPrototype", Boolean.class, PRIVATE)
        .defineField("isEagerInit", Supplier.class, PRIVATE)
        .implement(SmartFactoryBeanInterceptor.class).intercept(FieldAccessor.ofBeanProperty())
        .method(named("isSingleton").and(isDeclaredBy(FactoryBean.class))).intercept(MethodDelegation.toField("isSingleton"))
        .method(named("getObjectType").and(isDeclaredBy(FactoryBean.class)))
        .intercept(ObjectTypeProvider.class.isAssignableFrom(objectFactoryType) ? MethodCall.invokeSuper()
            : MethodCall.invoke(named("getObjectTypeClass")))
        .method(named("isPrototype").and(isDeclaredBy(SmartFactoryBean.class))).intercept(MethodDelegation.toField("isPrototype"))
        .method(named("isEagerInit").and(isDeclaredBy(SmartFactoryBean.class))).intercept(MethodDelegation.to(interceptor))
        .method(named("getObject").and(isDeclaredBy(FactoryBean.class))).intercept(MethodCall.invokeSuper())
        .make()
        .load(this.getClass().getClassLoader(), INJECTION)
        .getLoaded();
  }

  public class GenericInterceptor implements InvocationHandler {

    @Override
    @RuntimeType
    public Object invoke(@This Object object, @Origin Method method, @AllArguments Object[] args) throws Throwable {
      return ((SmartFactoryBeanInterceptor) object).getIsEagerInit().get();
    }
  }

  public interface SmartFactoryBeanInterceptor extends SmartFactoryBean {

    Boolean getIsSingleton();

    void setIsSingleton(Boolean isSingleton);

    Class getObjectTypeClass();

    void setObjectTypeClass(Class objectType);

    boolean getIsPrototype();

    void setIsPrototype(Boolean isPrototype);

    Supplier getIsEagerInit();

    void setIsEagerInit(Supplier isEagerInit);
  }

}
