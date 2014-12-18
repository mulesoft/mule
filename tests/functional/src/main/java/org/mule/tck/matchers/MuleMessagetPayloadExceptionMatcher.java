/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class MuleMessagetPayloadExceptionMatcher
{
    public static Matcher<MuleMessage> exceptionPayload(final Matcher<Object> exceptionPayloadMatcher) {
        return new FeatureMatcher<MuleMessage, Object>(
                exceptionPayloadMatcher, "exception payload", "exception payload") {
            @Override
            protected Object featureValueOf(MuleMessage actual){
                return actual.getExceptionPayload();
            }
        };
    }

    public static Matcher<Object> exception(final Matcher<Object> throwableMatcher) {
        return new FeatureMatcher<Object, Object>(
                throwableMatcher, "exception", "exception") {
            @Override
            protected Object featureValueOf(Object actual){
                ExceptionPayload exception = (ExceptionPayload) actual;
                return exception.getException();
            }
        };
    }
}
