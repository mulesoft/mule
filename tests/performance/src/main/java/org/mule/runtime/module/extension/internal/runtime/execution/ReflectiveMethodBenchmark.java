/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import org.mule.AbstractBenchmark;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Threads;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Loaded;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.matcher.ElementMatchers;

//@OutputTimeUnit(NANOSECONDS)
@Threads(3)
public class ReflectiveMethodBenchmark extends AbstractBenchmark {

  private static class Target {

    public int doIt(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9) {
      return arg0 + arg1 + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8 + arg9;

    }
  }

  private Target t;
  private Method method;

  @Setup
  public void setUp() throws NoSuchMethodException, SecurityException, FileNotFoundException, IOException {
    t = new Target();
    method = t.getClass().getDeclaredMethod("doIt", new Class[] {int.class, int.class, int.class, int.class, int.class, int.class,
        int.class, int.class, int.class, int.class});

    // final Loaded<Object> bb =
    final Unloaded<Object> bbMade = new ByteBuddy()
        .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
        .name("my.company.GeneratedByByteBuddy")
        .defineField("a0", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a1", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a2", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a3", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a4", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a5", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a6", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a7", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a8", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineField("a9", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
        .defineConstructor(Visibility.PUBLIC)
        .withParameter(int.class, "a0")
        .withParameter(int.class, "a1")
        .withParameter(int.class, "a2")
        .withParameter(int.class, "a3")
        .withParameter(int.class, "a4")
        .withParameter(int.class, "a5")
        .withParameter(int.class, "a6")
        .withParameter(int.class, "a7")
        .withParameter(int.class, "a8")
        .withParameter(int.class, "a9")
        .intercept(new Implementation() {

          @Override
          public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
          }

          @Override
          public ByteCodeAppender appender(Implementation.Target implementationTarget) {
            return new ByteCodeAppender() {

              @Override
              public Size apply(MethodVisitor methodVisitor,
                                Context instrumentationContext,
                                MethodDescription instrumentedMethod) {
                StackManipulation.Size size = new StackManipulation.Compound(
                                                                             MethodVariableAccess.REFERENCE.loadFrom(0),
                                                                             MethodInvocation
                                                                                 .invoke(new TypeDescription.ForLoadedType(Object.class)
                                                                                     .getDeclaredMethods()
                                                                                     .filter(isConstructor()
                                                                                         .and(takesArguments(0)))
                                                                                     .getOnly()),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(0),
                                                                             MethodVariableAccess.INTEGER.loadFrom(1),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a0")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(1),
                                                                             MethodVariableAccess.INTEGER.loadFrom(2),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a1")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(2),
                                                                             MethodVariableAccess.INTEGER.loadFrom(3),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a2")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(3),
                                                                             MethodVariableAccess.INTEGER.loadFrom(4),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a3")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(4),
                                                                             MethodVariableAccess.INTEGER.loadFrom(5),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a4")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(5),
                                                                             MethodVariableAccess.INTEGER.loadFrom(6),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a5")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(6),
                                                                             MethodVariableAccess.INTEGER.loadFrom(7),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a6")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(7),
                                                                             MethodVariableAccess.INTEGER.loadFrom(8),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a7")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(8),
                                                                             MethodVariableAccess.INTEGER.loadFrom(9),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a8")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(9),
                                                                             MethodVariableAccess.INTEGER.loadFrom(10),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a9")).getOnly())
                                                                                 .write(),
                                                                             MethodReturn.VOID).apply(methodVisitor,
                                                                                                      instrumentationContext);
                return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
              }
            };
          }
        })
        .defineMethod("doIt", int.class, Visibility.PUBLIC)
        .intercept(new Implementation() {

          @Override
          public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
          }

          @Override
          public ByteCodeAppender appender(Implementation.Target implementationTarget) {
            return new ByteCodeAppender() {

              @Override
              public Size apply(MethodVisitor methodVisitor,
                                Context instrumentationContext,
                                MethodDescription instrumentedMethod) {
                StackManipulation.Size size = new StackManipulation.Compound(
                                                                             MethodVariableAccess.REFERENCE.loadFrom(0),
                                                                             MethodInvocation
                                                                                 .invoke(new TypeDescription.ForLoadedType(Object.class)
                                                                                     .getDeclaredMethods()
                                                                                     .filter(isConstructor()
                                                                                         .and(takesArguments(0)))
                                                                                     .getOnly()),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(0),
                                                                             MethodVariableAccess.INTEGER.loadFrom(1),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a0")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(1),
                                                                             MethodVariableAccess.INTEGER.loadFrom(2),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a1")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(2),
                                                                             MethodVariableAccess.INTEGER.loadFrom(3),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a2")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(3),
                                                                             MethodVariableAccess.INTEGER.loadFrom(4),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a3")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(4),
                                                                             MethodVariableAccess.INTEGER.loadFrom(5),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a4")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(5),
                                                                             MethodVariableAccess.INTEGER.loadFrom(6),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a5")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(6),
                                                                             MethodVariableAccess.INTEGER.loadFrom(7),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a6")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(7),
                                                                             MethodVariableAccess.INTEGER.loadFrom(8),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a7")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(8),
                                                                             MethodVariableAccess.INTEGER.loadFrom(9),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a8")).getOnly())
                                                                                 .write(),
                                                                             MethodVariableAccess.REFERENCE.loadFrom(9),
                                                                             MethodVariableAccess.INTEGER.loadFrom(10),
                                                                             FieldAccess
                                                                                 .forField(implementationTarget
                                                                                     .getInstrumentedType()
                                                                                     .getDeclaredFields()
                                                                                     .filter(named("a9")).getOnly())
                                                                                 .write(),
                                                                             MethodReturn.VOID).apply(methodVisitor,
                                                                                                      instrumentationContext);
                return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
              }
            };
          }
        })
        .make();



    final File file = new File("Lalala.class");
    System.out.println(file.getAbsolutePath());
    try (FileOutputStream os = new FileOutputStream(file)) {
      os.write(bbMade.getBytes());
    }
    // .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION);
  }


  @Benchmark
  public Object reflectionCall() {
    try {
      return method.invoke(t, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Benchmark
  public Object directCall() {
    return t.doIt(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
  }

  // @Benchmark
  // public Object byteBuddyDirectCall() {
  //
  // }
}
