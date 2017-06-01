/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.resource;

import java.net.URL;

public class ResourceConsumer {
    public ResourceConsumer() {
    }

    public String consumeResource(Object payload) {
        URL resource = this.getClass().getResource("/META-INF/app-resource.txt");

        if (resource == null) {
            throw new IllegalStateException("Error reading app resource");
        }

        return "success";
    }
}
