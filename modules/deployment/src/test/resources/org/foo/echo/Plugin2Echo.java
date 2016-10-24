/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.echo;

import org.bar.BarUtils;

public class Plugin2Echo {
    public Plugin2Echo() {
    }

    public String echo(String data) {
        return (new BarUtils()).doStuff();
    }
}
