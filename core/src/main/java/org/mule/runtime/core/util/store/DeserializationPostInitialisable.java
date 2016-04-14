/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A marker interface used to trigger post-deserialization initialization of an object. This works
 * in the same way as {@link Cloneable} interface. Implementors of this interface must add the
 * method <code>private void initAfterDeserialization(MuleContext muleContext) throws MuleException</code>
 * to their class (note that it's private). This will get invoked after the object has been
 * deserialized passing in the current mulecontext when using either
 * {@link org.mule.transformer.wire.SerializationWireFormat},
 * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat}, or the
 * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
 *
 * @see org.mule.transformer.simple.ByteArrayToSerializable
 * @see org.mule.transformer.wire.SerializationWireFormat
 * @see org.mule.transformer.wire.SerializedMuleMessageWireFormat
 */
public interface DeserializationPostInitialisable
{
    public class Implementation
    {
        public static void init(final Object object, final MuleContext muleContext) throws Exception
        {
            try
            {
                final Method m = object.getClass().getDeclaredMethod("initAfterDeserialisation",
                    MuleContext.class);

                Object o = AccessController.doPrivileged(new PrivilegedAction<Object>()
                {
                    @Override
                    public Object run()
                    {
                        try
                        {
                            m.setAccessible(true);
                            m.invoke(object, muleContext);
                            return null;
                        }
                        catch (Exception e)
                        {
                            return e;
                        }

                    }
                });

                if (o != null)
                {
                    throw (Exception) o;
                }

            }
            catch (NoSuchMethodException e)
            {
                throw new IllegalArgumentException("Object " + object.getClass() + " implements " +
                        DeserializationPostInitialisable.class + " but does not have a method " +
                        "private void initAfterDeserialisation(MuleContext) defined", e);
            }
        }
    }
}
