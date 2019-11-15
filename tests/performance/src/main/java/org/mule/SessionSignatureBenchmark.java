/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.mule.api.MuleRuntimeException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Benchmark)
@Fork(1)
//@BenchmarkMode(Throughput)
@BenchmarkMode(Throughput)
@OutputTimeUnit(SECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class SessionSignatureBenchmark
{
    private static final Mac MAC_SIGNER;

    static
    {
        try
        {
            String algorithm = "HmacSHA256";
            MAC_SIGNER = Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec("ThoughtYouHeardFootstepsBehind".getBytes(UTF_8), algorithm);
            MAC_SIGNER.init(secretKeySpec);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Benchmark
    @Threads(1)
    public byte[] baselineSingleThread() {
        return generateRandomData(256);
    }
    
    @Benchmark
    @Threads(1)
    public byte[] synchronizedSignSingleThread() {
        synchronized (SessionSignatureBenchmark.class)
        {
            return MAC_SIGNER.doFinal(generateRandomData(256));
        }
    }
    
    @Benchmark
    @Threads(1)
    public byte[] clonedSignerSingleThread() throws CloneNotSupportedException {
        return ((Mac) MAC_SIGNER.clone()).doFinal(generateRandomData(256));
    }
    
    @Benchmark
    public byte[] baseline() {
        return generateRandomData(256);
    }
    
    @Benchmark
    public byte[] synchronizedSign() {
        synchronized (SessionSignatureBenchmark.class)
        {
            return MAC_SIGNER.doFinal(generateRandomData(256));
        }
    }
    
    @Benchmark
    public byte[] clonedSigner() throws CloneNotSupportedException {
        return ((Mac) MAC_SIGNER.clone()).doFinal(generateRandomData(256));
    }

    private byte[] generateRandomData(int length)
    {
        Blackhole.consumeCPU(40000000);
        
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return array;
    }
}
