/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
 package org.guiceyfruit.mule;
 
 import org.mule.api.MuleContext;
 import org.mule.api.lifecycle.Initialisable;
 
 import com.google.inject.Guice;
 import com.google.inject.Injector;
 import com.google.inject.Module;
 import com.google.inject.ProvisionException;
 import com.google.inject.TypeLiteral;
 import com.google.inject.internal.Iterables;
 import com.google.inject.matcher.Matchers;
 import com.google.inject.spi.InjectionListener;
 import com.google.inject.spi.TypeEncounter;
 import com.google.inject.spi.TypeListener;
 
 import java.util.Arrays;
 import java.util.Collections;
 
 import org.guiceyfruit.jsr250.Jsr250Module;
 import org.guiceyfruit.mule.support.DisposableCloser;
 
 /**
  * @deprecated Guice module is deprecated and will be removed in Mule 4.
  */
 @Deprecated
 public class MuleModule extends Jsr250Module
 {
     /**
      * Returns a new Injector with support for
      * Mule lifecycle support along with JSR 250 support included.
      */
     public static Injector createInjector(Module... modules)
     {
         Iterable<? extends Module> iterable = Iterables.concat(
                 Collections.singletonList(new MuleModule()), Arrays.asList(modules));
 
         return Guice.createInjector(iterable);
     }
 
     protected void configure()
     {
         super.configure();
 
         bindListener(Matchers.any(), new TypeListener()
         {
             public <I> void hear(TypeLiteral<I> injectableType, TypeEncounter<I> encounter)
             {
 
                 encounter.register(new InjectionListener<I>()
                 {
                     public void afterInjection(I injectee)
                     {
                         if (injectee instanceof Initialisable && !(injectee instanceof MuleContext))
                         {
                             Initialisable initialisable = (Initialisable) injectee;
                             try
                             {
                                 initialisable.initialise();
                             }
                             catch (Exception e)
                             {
                                 throw new ProvisionException("Failed to invoke initialise(): " + e, e);
                             }
                         }
                     }
                 });
 
             }
         });
 
         bind(DisposableCloser.class);
     }
 
 }
