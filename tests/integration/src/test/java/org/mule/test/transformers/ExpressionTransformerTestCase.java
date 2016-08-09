/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.expression.transformers.ExpressionTransformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import groovyjarjarasm.asm.ClassWriter;
import groovyjarjarasm.asm.Opcodes;
import org.junit.Test;

public class ExpressionTransformerTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testExpressionEvaluationClassLoaderEL() throws ClassNotFoundException, TransformerException {
    ExpressionTransformer transformer = new ExpressionTransformer();
    transformer.setMuleContext(muleContext);
    transformer.addArgument(new ExpressionArgument("test", new ExpressionConfig("payload is org.MyClass"), false));

    withContextClassLoader(new MyClassClassLoader(), () -> {
      try {
        transformer.initialise();
      } catch (Exception e) {
        fail(e.getMessage());
      }
    });

    assertFalse((Boolean) transformer.transform("test"));
  }

  @Test
  public void testNullPayloadIsConsideredAsNullResultEL() throws Exception {
    ExpressionTransformer transformer = new ExpressionTransformer();
    transformer.setMuleContext(muleContext);
    transformer.setReturnSourceIfNull(true);
    ExpressionConfig config = new ExpressionConfig("null");

    // MVL doesn't return NullPayload but rather null. So 'optional' needs to be true.
    ExpressionArgument argument = new ExpressionArgument("test", config, true);
    argument.setMuleContext(muleContext);
    transformer.addArgument(argument);

    MuleEvent event = getTestEvent("Test");
    Object result = transformer.transformMessage(event, null);
    assertTrue(result instanceof MuleMessage);
    MuleMessage transformedMessage = (MuleMessage) result;

    assertEquals("Test", transformedMessage.getPayload());

  }

  class MyClassClassLoader extends ClassLoader {

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
      if (className.equals("org.MyClass")) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "org/MyClass", null, "java/lang/Object", null);
        return defineClass(className, cw.toByteArray(), 0, cw.toByteArray().length);
      } else {
        return super.findClass(className);
      }
    }
  }
}
