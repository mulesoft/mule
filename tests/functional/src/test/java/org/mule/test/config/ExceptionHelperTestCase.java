/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.mule.api.DefaultMuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.registry.ResolverException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class ExceptionHelperTestCase extends AbstractMuleTestCase
{

    @Test
    public void testNestedExceptionRetreval() throws Exception
    {
        Exception testException = getException();
        Throwable t = ExceptionHelper.getRootException(testException);
        assertNotNull(t);
        assertEquals("blah", t.getMessage());
        assertNull(t.getCause());

        t = ExceptionHelper.getRootMuleException(testException);
        assertNotNull(t);
        assertEquals("bar", t.getMessage());
        assertNotNull(t.getCause());

        List<?> l = ExceptionHelper.getExceptionsAsList(testException);
        assertEquals(3, l.size());

        Map<?, ?> info = ExceptionHelper.getExceptionInfo(testException);
        assertNotNull(info);
        assertEquals(1, info.size());
        assertNotNull(info.get("JavaDoc"));
    }
    
    @Test
    public void testSummarizeWithDepthBeyondStackTraceLength()
    {
        Exception exception = getException();
        int numberOfStackFrames = exception.getStackTrace().length;
        int depth = numberOfStackFrames + 1;
        
        Throwable summary = ExceptionHelper.summarise(exception, depth);
        assertNotNull(summary);
    }

    @Test
    public void testGetNonMuleExceptionCause()
    {
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(), null)), IsNull.<Object>nullValue());
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(),
                new ConfigurationException(CoreMessages.failedToBuildMessage(), null))), IsNull.<Object>nullValue());
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(),
                new ConfigurationException(CoreMessages.failedToBuildMessage(),
                        new IllegalArgumentException()))), IsInstanceOf.instanceOf(IllegalArgumentException.class));
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(),
                new ConfigurationException(CoreMessages.failedToBuildMessage(),
                        new IllegalArgumentException(new NullPointerException())))), IsInstanceOf.instanceOf(IllegalArgumentException.class));
        assertThat(ExceptionHelper.getNonMuleException(new IllegalArgumentException()),IsInstanceOf.instanceOf(IllegalArgumentException.class));
    }

    private Exception getException()
    {
        return new DefaultMuleException(MessageFactory.createStaticMessage("foo"), new DefaultMuleException(
            MessageFactory.createStaticMessage("bar"), new Exception("blah")));
    }
}
