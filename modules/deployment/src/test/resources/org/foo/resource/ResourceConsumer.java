/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.resource;

import org.mule.runtime.core.util.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

public class ResourceConsumer {
    public ResourceConsumer() {
    }

    public List<String> consumeResource(Object payload) {
        URL resource = this.getClass().getResource("/META-INF/app-resource.txt");

        try {
            return FileUtils.readLines(new File(resource.toURI()));
        } catch (Exception var4) {
            throw new IllegalStateException("Error reading app resource", var4);
        }
    }
}
