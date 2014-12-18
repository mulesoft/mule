/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import java.io.IOException;

import javax.activation.DataHandler;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class MuleMessagetInboundAttachmentContentMatcher
{

    public static Matcher<DataHandler> content(final Matcher<Object> contentMatcher) {
        return new FeatureMatcher<DataHandler, Object>(
                contentMatcher, "content", "content") {
            @Override
            protected Object featureValueOf(DataHandler actual){
                try
                {
                    return actual.getContent();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    public static Matcher<Object> asString(final Matcher<String> contentMatcher) {
        return new FeatureMatcher<Object, String>(
                contentMatcher, "as String", "as String") {
            @Override
            protected String featureValueOf(Object actual){
                return actual.toString();
            }
        };
    }
}
