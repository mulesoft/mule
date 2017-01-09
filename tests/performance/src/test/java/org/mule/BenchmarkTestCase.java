/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.runner.RunnerException;

/**
 * A number of benchmarks have no assertions currently in 3.x, so just ensure all benchamrks run OK.
 */
public class BenchmarkTestCase extends AbstractMuleTestCase
{

    @Test
    public void runBenchmarksWithoutAssertions() throws RunnerException, IOException
    {
        Main.main(new String[] {"-f", "0", "-bm", "ss", "-wi", "0", "-i", "1"});
    }

}
