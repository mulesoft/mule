/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.core.internal.util.MultiParentClassLoaderUtils.multiParentClassLoaderFor;

import static java.lang.Class.forName;

import static net.bytebuddy.description.modifier.Ownership.STATIC;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.IMITATE_SUPER_CLASS;
import static net.bytebuddy.implementation.FieldAccessor.ofBeanProperty;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.implementation.MethodCall.invokeSuper;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.implementation.MethodDelegation.toField;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;

import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectTypeProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

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

  public static final String IS_SINGLETON = "isSingleton";
  public static final String OBJECT_TYPE_CLASS = "objectTypeClass";
  public static final String IS_PROTOTYPE = "isPrototype";
  public static final String IS_EAGER_INIT = "isEagerInit";
  private final ByteBuddy byteBuddy = new ByteBuddy();
  private final IsEagerInitGetterInterceptor interceptor = new IsEagerInitGetterInterceptor();

  /**
   * Retrieves a {@link Class} for the {@link ObjectFactory} defined by the {@code objectFactoryType} parameter. Once acquired the
   * {@code Class} instance should not be reused for another {@link ComponentBuildingDefinition}.
   *
   * @param objectFactoryType the {@link ObjectFactory} of the component
   * @param objectTypeClass   the class of the object that the factory will instantiate
   * @return the {@code FactoryBean} class to be used by spring for the provided configuration.
   */
  public Class<ObjectFactory> getObjectFactoryClass(Class objectFactoryType, Class objectTypeClass) {
    synchronized (this.getClass().getClassLoader()) {
      String name;
      boolean callingSuper = ObjectTypeProvider.class.isAssignableFrom(objectFactoryType);

      // Make sure the generated class exists in a module that has visibility on all the required superclasses and interfaces.
      // spring-config has that scope.
      String packageName = this.getClass().getPackage().getName();
      if (callingSuper) {
        name = packageName + "." +
            objectFactoryType.getSimpleName() + "_ByteBuddy_CallingSuperGetObjectType";
      } else {
        name = packageName + "." +
            objectFactoryType.getSimpleName() + "_ByteBuddy_" + objectTypeClass.getName().replace(".", "_");
      }

      ClassLoader classLoader = multiParentClassLoaderFor(objectFactoryType.getClassLoader());
      try {
        return (Class<ObjectFactory>) forName(name, true, classLoader);
      } catch (ClassNotFoundException e) {
        // class doesn't exist, generate
      }
      final Class createObjectFactoryDynamicClass =
          createObjectFactoryDynamicClass(objectFactoryType, name, classLoader, callingSuper, objectTypeClass);
      return createObjectFactoryDynamicClass;
    }
  }

  private Class<ObjectFactory> createObjectFactoryDynamicClass(Class objectFactoryType, String name, ClassLoader classLoader,
                                                               boolean callingSuper, Class objectTypeClass) {

    return byteBuddy
        .subclass(objectFactoryType, IMITATE_SUPER_CLASS)
        .name(name)
        // W-12362157: set the objectTypeClass field static so spring can get it without fully initializing the object
        .defineField(OBJECT_TYPE_CLASS, Class.class, PRIVATE, STATIC)
        .initializer(new LoadedTypeInitializer.ForStaticField(OBJECT_TYPE_CLASS, objectTypeClass))
        // Add fields to set properties.
        .defineField(IS_SINGLETON, Boolean.class, PRIVATE)
        .defineField(IS_PROTOTYPE, Boolean.class, PRIVATE)
        .defineField(IS_EAGER_INIT, Supplier.class, PRIVATE)
        // Implements the SmartFactoryBeanInterceptor interface to add getters and setters for the fields. This interface extends
        // from SmartFactoryBean.
        .implement(SmartFactoryBeanInterceptor.class).intercept(ofBeanProperty())
        // Implements the SmartFactoryBean methods and delegates to the fields.
        .method(named(IS_SINGLETON).and(isDeclaredBy(FactoryBean.class))).intercept(toField(IS_SINGLETON))
        .method(named("getObjectType").and(isDeclaredBy(FactoryBean.class)))
        .intercept(callingSuper ? invokeSuper() : invoke(named("getObjectTypeClass")))
        .method(named(IS_PROTOTYPE).and(isDeclaredBy(SmartFactoryBean.class))).intercept(toField(IS_PROTOTYPE))
        .method(named(IS_EAGER_INIT).and(isDeclaredBy(SmartFactoryBean.class))).intercept(to(interceptor))
        .method(named("getObject").and(isDeclaredBy(FactoryBean.class))).intercept(invokeSuper())
        // Create the class and inject it in the current classloader
        .make()
        .load(classLoader, INJECTION)
        .getLoaded();
  }


  protected static class IsEagerInitGetterInterceptor implements InvocationHandler {

    @Override
    @RuntimeType
    public Object invoke(@This Object object, @Origin Method method, @AllArguments Object[] args) throws Throwable {
      return ((SmartFactoryBeanInterceptor) object).getIsEagerInit().get();
    }
  }

  /**
   * This interface is used to implement the getters and setters of the fields added with Byte Buddy. It also extends from
   * {@link SmartFactoryBean}.
   *
   * @since 4.5.0
   */
  public interface SmartFactoryBeanInterceptor extends SmartFactoryBean {

    Boolean getIsSingleton();

    void setIsSingleton(Boolean isSingleton);

    Class getObjectTypeClass();

    boolean getIsPrototype();

    void setIsPrototype(Boolean isPrototype);

    Supplier getIsEagerInit();

    void setIsEagerInit(Supplier isEagerInit);
  }

}
