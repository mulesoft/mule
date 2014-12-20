/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;

import org.apache.commons.io.IOUtils;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class MuleMessagetInboundAttachmentInputStreamMatcher
{
    public static Matcher<DataHandler> inputStream(final Matcher<InputStream> inputStreamtMatcher) {
        return new FeatureMatcher<DataHandler, InputStream>(
                inputStreamtMatcher, "content", "content") {
            @Override
            protected InputStream featureValueOf(DataHandler actual){
                try
                {
                    return actual.getInputStream();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    public static Matcher<InputStream> asString(final Matcher<String> contentMatcher) {

        return new FeatureMatcher<InputStream, String>(
                contentMatcher, "as String", "as String") {

            @Override
            protected String featureValueOf(InputStream actual){
                try
                {
                    return IOUtils.toString(actual);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }
}
