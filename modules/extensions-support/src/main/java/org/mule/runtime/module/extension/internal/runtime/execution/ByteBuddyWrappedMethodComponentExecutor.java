/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.util.Arrays.asList;
import static net.bytebuddy.description.modifier.FieldManifestation.FINAL;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static net.bytebuddy.description.type.TypeDescription.Generic.Builder.parameterizedType;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

/**
 * Executes a task associated to a {@link ExecutionContext} by invoking a given {@link Method} from a dynamically generated class.
 *
 * @param <M> the generic type of the associated {@link ComponentModel}
 * @since 4.0
 */
public class ByteBuddyWrappedMethodComponentExecutor<M extends ComponentModel>
    implements MuleContextAware, Lifecycle, OperationArgumentResolverFactory<M> {

  private static class NoArgumentsResolverDelegate implements ArgumentResolverDelegate {

    private static final Supplier[] EMPTY = new Supplier[] {};

    @Override
    public ArgumentResolver<?>[] getArgumentResolvers() {
      return new ArgumentResolver[] {};
    }

    @Override
    public Supplier<Object>[] resolve(ExecutionContext executionContext, Class<?>[] parameterTypes) {
      return EMPTY;
    }
  }

  private static final Logger LOGGER = getLogger(ByteBuddyWrappedMethodComponentExecutor.class);
  private static final ArgumentResolverDelegate NO_ARGS_DELEGATE = new NoArgumentsResolverDelegate();

  private final List<ParameterGroupModel> groups;

  private MethodComponentExecutorWrapper wrapper;
  private final Method method;
  private final Object componentInstance;
  private final ClassLoader extensionClassLoader;

  private ArgumentResolverDelegate argumentResolverDelegate;

  private MuleContext muleContext;

  public ByteBuddyWrappedMethodComponentExecutor(List<ParameterGroupModel> groups, Method method, Object componentInstance) {
    this.groups = groups;
    this.method = method;
    this.componentInstance = componentInstance;
    extensionClassLoader = method.getDeclaringClass().getClassLoader();
  }

  public Object execute(ExecutionContext<M> executionContext) {
    return withContextClassLoader(extensionClassLoader, () -> wrapper.execute(executionContext), RuntimeException.class, e -> {
      // Use an exception type proper to extensions
      throw new UndeclaredThrowableException(e);
    });
    // () -> invokeMethod(method, componentInstance,
    // stream(getParameterValues(executionContext, method.getParameterTypes()))
    // .map(Supplier::get).toArray(Object[]::new)));
  }

  private Supplier<Object>[] getParameterValues(ExecutionContext<M> executionContext, Class<?>[] parameterTypes) {
    return argumentResolverDelegate.resolve(executionContext, parameterTypes);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(componentInstance, true, muleContext);

    argumentResolverDelegate =
        isEmpty(method.getParameterTypes()) ? NO_ARGS_DELEGATE : getMethodArgumentResolver(groups, method);

    Builder<Object> operationWrapperClassBuilder = new ByteBuddy()
        .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
        .implement(MethodComponentExecutorWrapper.class)
        .name(method.getDeclaringClass().getName() + "$" + method.getName() + "$" + (this.hashCode())
            + "$MethodComponentExecutorWrapper")
        .defineField("componentInstance", method.getDeclaringClass(), PRIVATE, FINAL);

    for (int i = 0; i < method.getParameterTypes().length; ++i) {
      operationWrapperClassBuilder = operationWrapperClassBuilder
          .defineField(method.getParameters()[i].getName() + "Resolver",
                       parameterizedType(ArgumentResolver.class, method.getParameterTypes()[i]).build(),
                       PRIVATE, FINAL);
    }

    Annotatable<Object> constructorDefinition = operationWrapperClassBuilder
        .defineConstructor(PUBLIC)
        .withParameter(method.getDeclaringClass(), "componentInstance");
    for (int i = 0; i < method.getParameterTypes().length; ++i) {
      constructorDefinition = constructorDefinition
          .withParameter(parameterizedType(ArgumentResolver.class, method.getParameterTypes()[i]).build(),
                         method.getParameters()[i].getName() + "Resolver");
    }

    final Unloaded<Object> byteBuddyMadeWrapper = constructorDefinition.intercept(new Implementation() {

      @Override
      public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
      }

      @Override
      public ByteCodeAppender appender(Target implementationTarget) {
        return (methodVisitor, instrumentationContext, instrumentedMethod) -> {
      List<StackManipulation> stackManipulationItems = new ArrayList<>();

      stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
      stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Object.class)
        .getDeclaredMethods()
        .filter(isConstructor().and(takesArguments(0)))
        .getOnly()));
      stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
      stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(1));
      stackManipulationItems.add(FieldAccess.forField(implementationTarget
        .getInstrumentedType()
        .getDeclaredFields()
        .filter(named("componentInstance")).getOnly())
        .write());

      for (int i = 0; i < method.getParameterTypes().length; ++i) {
       stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
       stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(i + 2));
       stackManipulationItems.add(FieldAccess.forField(implementationTarget
         .getInstrumentedType()
         .getDeclaredFields()
         .filter(named(method.getParameters()[i].getName() + "Resolver")).getOnly())
         .write());
      }

      stackManipulationItems.add(MethodReturn.VOID);

      StackManipulation.Size size = new StackManipulation.Compound(stackManipulationItems)
        .apply(methodVisitor, instrumentationContext);
      return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
     };
      }
    })
        .defineMethod("execute", Object.class, PUBLIC)
        .withParameter(ExecutionContext.class, "executionContext")
        .throwing(method.getExceptionTypes())
        .intercept(new Implementation() {

          @Override
          public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
          }

          @Override
          public ByteCodeAppender appender(Target implementationTarget) {
            return (methodVisitor, instrumentationContext, instrumentedMethod) -> {
        List<StackManipulation> stackManipulationItems = new ArrayList<>();

        stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
        stackManipulationItems.add(FieldAccess.forField(implementationTarget
          .getInstrumentedType()
          .getDeclaredFields()
          .filter(named("componentInstance")).getOnly())
          .read());

        for (int i = 0; i < method.getParameterTypes().length; ++i) {
         stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(0));
         stackManipulationItems.add(FieldAccess.forField(implementationTarget
           .getInstrumentedType()
           .getDeclaredFields()
           .filter(named(method.getParameters()[i].getName() + "Resolver")).getOnly())
           .read());
         stackManipulationItems.add(MethodVariableAccess.REFERENCE.loadFrom(1));
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(ArgumentResolver.class)
           .getDeclaredMethods()
           .filter(named("resolve"))
           .getOnly()));
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Supplier.class)
           .getDeclaredMethods()
           .filter(named("get"))
           .getOnly()));
         final TypeDescription.ForLoadedType paramType =
           new TypeDescription.ForLoadedType(method.getParameterTypes()[i]);

         if (paramType.isPrimitive()) {
          stackManipulationItems.add(TypeCasting.to(paramType.asBoxed()));

          if (int.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Integer.class)
             .getDeclaredMethods()
             .filter(named("intValue"))
             .getOnly()));
          } else if (long.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Long.class)
             .getDeclaredMethods()
             .filter(named("longValue"))
             .getOnly()));
          } else if (short.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Short.class)
             .getDeclaredMethods()
             .filter(named("shortValue"))
             .getOnly()));
          } else if (double.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Double.class)
             .getDeclaredMethods()
             .filter(named("doubleValue"))
             .getOnly()));
          } else if (float.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Float.class)
             .getDeclaredMethods()
             .filter(named("floatValue"))
             .getOnly()));
          } else if (boolean.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Boolean.class)
             .getDeclaredMethods()
             .filter(named("booleanValue"))
             .getOnly()));
          } else if (char.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Character.class)
             .getDeclaredMethods()
             .filter(named("charValue"))
             .getOnly()));
          } else if (byte.class.equals(method.getParameterTypes()[i])) {
           stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Byte.class)
             .getDeclaredMethods()
             .filter(named("byteValue"))
             .getOnly()));
          }
         } else {
          stackManipulationItems.add(TypeCasting.to(paramType));
         }
        }

        stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(method.getDeclaringClass())
          .getDeclaredMethods()
          .filter(named(method.getName()))
          .getOnly()));

        if (Void.TYPE.equals(method.getReturnType())) {
         stackManipulationItems.add(NullConstant.INSTANCE);
        } else if (int.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Integer.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, int.class)))
           .getOnly()));
        } else if (long.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Long.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, long.class)))
           .getOnly()));
        } else if (short.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Short.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, short.class)))
           .getOnly()));
        } else if (double.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Double.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, double.class)))
           .getOnly()));
        } else if (float.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Float.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, float.class)))
           .getOnly()));
        } else if (boolean.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Boolean.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, boolean.class)))
           .getOnly()));
        } else if (char.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Character.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, char.class)))
           .getOnly()));
        } else if (byte.class.equals(method.getReturnType())) {
         stackManipulationItems.add(MethodInvocation.invoke(new TypeDescription.ForLoadedType(Byte.class)
           .getDeclaredMethods()
           .filter(named("valueOf").and(takesArgument(0, byte.class)))
           .getOnly()));
        }
        stackManipulationItems.add(MethodReturn.of(new TypeDescription.ForLoadedType(Object.class)));

        StackManipulation.Size size = new StackManipulation.Compound(stackManipulationItems)
          .apply(methodVisitor, instrumentationContext);
        return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
       };
          }
        }).make();

    byteBuddyMadeWrapper.getBytes();
    final File file =
        new File(method.getDeclaringClass().getName() + "$" + method.getName() + (this.hashCode())
            + "$MethodComponentExecutorWrapper" + ".class");
    try (FileOutputStream os = new FileOutputStream(file)) {
      os.write(byteBuddyMadeWrapper.getBytes());
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // System.out.println(file.getAbsolutePath());

    final Class<? extends Object> loadedWrapperClass = byteBuddyMadeWrapper
        .load(new CompositeClassLoader(ByteBuddyWrappedMethodComponentExecutor.class.getClassLoader(),
                                       extensionClassLoader),
              INJECTION)
        .getLoaded();

    List<Object> ctorArgs = new ArrayList<>();
    ctorArgs.add(componentInstance);
    ctorArgs.addAll(asList(argumentResolverDelegate.getArgumentResolvers()));

    try {
      wrapper = (MethodComponentExecutorWrapper) loadedWrapperClass.getConstructors()[0]
          .newInstance(ctorArgs.toArray(new Object[ctorArgs.size()]));
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | SecurityException e) {
      throw new InitialisationException(e, this);
    }
  }

  private ArgumentResolverDelegate getMethodArgumentResolver(List<ParameterGroupModel> groups, Method method) {
    try {
      MethodArgumentResolverDelegate resolver = new MethodArgumentResolverDelegate(groups, method);
      initialiseIfNeeded(resolver, muleContext);
      return resolver;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not initialize argument resolver resolver"), e);
    }
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(componentInstance);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(componentInstance);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(componentInstance, LOGGER);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
    if (componentInstance instanceof MuleContextAware) {
      ((MuleContextAware) componentInstance).setMuleContext(context);
    }
  }

  @Override
  public Function<ExecutionContext<M>, Map<String, Object>> createArgumentResolver(M operationModel) {
    return ec -> withContextClassLoader(extensionClassLoader,
                                        () -> {
                                          final Object[] resolved =
                                              getParameterValues(ec, method.getParameterTypes());

                                          final Map<String, Object> resolvedParams = new HashMap<>();
                                          for (int i = 0; i < method.getParameterCount(); ++i) {
                                            resolvedParams.put(method.getParameters()[i].getName(), resolved[i]);
                                          }
                                          return resolvedParams;
                                        });
  }

  public interface MethodComponentExecutorWrapper {

    Object execute(ExecutionContext executionContext) throws Exception;
  }
}
