/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.executor.VoidReturnDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class VoidReturnDelegateTestCase extends AbstractMuleTestCase
{

    @Test
    public void returnsMuleEvent()
    {
        MuleEvent event = mock(MuleEvent.class);
        OperationContextAdapter operationContext = mock(OperationContextAdapter.class);
        when(operationContext.getEvent()).thenReturn(event);

        Object returnValue = VoidReturnDelegate.INSTANCE.asReturnValue(new Object(), operationContext);
        assertThat(event, is(sameInstance(returnValue)));
    }
}
