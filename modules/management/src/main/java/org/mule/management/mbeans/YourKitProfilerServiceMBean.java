/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import com.yourkit.api.ProfilingModes;

public interface YourKitProfilerServiceMBean
{
    public static final long ALLOCATION_RECORDING_ALL = ProfilingModes.ALLOCATION_RECORDING_ALL;
    public static final long ALLOCATION_RECORDING_ADAPTIVE = ProfilingModes.ALLOCATION_RECORDING_ADAPTIVE;
    public static final long CPU_SAMPLING = ProfilingModes.CPU_SAMPLING;
    public static final long CPU_TRACING = ProfilingModes.CPU_TRACING;
    public static final long MONITOR_PROFILING = ProfilingModes.MONITOR_PROFILING;
    public static final long CPU_J2EE = ProfilingModes.CPU_J2EE;
    public static final long SNAPSHOT_WITHOUT_HEAP = ProfilingModes.SNAPSHOT_WITHOUT_HEAP;
    public static final long SNAPSHOT_WITH_HEAP = ProfilingModes.SNAPSHOT_WITH_HEAP;
    public static final long SNAPSHOT_HPROF = ProfilingModes.SNAPSHOT_HPROF;

    /**
     * @return name of host where controlled profiled application is running. The method never returns null.
     */
    public String getHost();

    /**
     * @return port profiler agent listens on.
     */
    public int getPort();

    /**
     * This method is just a convenient replacement of captureSnapshot(YourKitProfilerServiceMBean.SNAPSHOT_WITH_HEAP)
     * @return absolute path to the captured snapshot.
     */
    public String captureMemorySnapshot() throws Exception;

    /**
     *  Captures snapshot: write profiling information to file.
     *
     * If some profiling is being performed (e.g. started with {@link #startCPUProfiling(long, String)}, {@link #startMonitorProfiling()}
     * or {@link #startAllocationRecording(long)}, or remotely), it won't stop after the capture. To stop it, explicitly call
     * {@link #stopCPUProfiling()}, {@link #stopMonitorProfiling()} or {@link #stopAllocationRecording()}.
     * @param snapshotFlags defines how much information should be stored:
     *        <ul>
     *        <li>YourKitProfilerServiceMBean.SNAPSHOT_WITHOUT_HEAP - capture snapshot with all the recorded information
     *            (CPU profiling, monitors, telemetry), but without the heap dump.
     *        <li>YourKitProfilerServiceMBean.SNAPSHOT_WITH_HEAP - capture snapshot with all the recorded information
     *            (CPU profiling, monitors, telemetry, allocations), as well as with the heap dump.
     *        <li>YourKitProfilerServiceMBean.SNAPSHOT_HPROF - capture snapshot in HPROF format 
     *            (it will including the heap dump only).
     *        </ul>
     * @return absolute path to the captured snapshot.
     * @throws Exception if capture failed. The possible reasons are:
     *        <ul>
     *        <li>there's no Java application with properly configured profiler agent listening on port at host
     *        <li>profiled application has terminated
     *        <li>agent cannot capture snapshot for some reason
     *        </ul>
     */
    public String captureSnapshot(long snapshotFlags) throws Exception;

    /**
     * Advance current object generation number.
     * Since that moment, all newly created objects will belong to the new generation.
     * Note that generations are also automatically advanced on capturing snapshots.
     * Generations are only available if the profiled application runs on Java 5 or newer.
     * @param description optional description associated with the generation
     */
    public void advanceGeneration(String description);

    /**
     * Start object allocation recording.
     * @param mode YourKitProfilerServiceMBean.ALLOCATION_RECORDING_ALL or YourKitProfilerServiceMBean.ALLOCATION_RECORDING_ADAPTIVE
     * @throws Exception if capture failed. The possible reasons are:
     *        <ul>
     *        <li>there's no Java application with properly configured profiler agent listening on port at host
     *        <li>profiled application has terminated
     *        <li>agent cannot capture snapshot for some reason
     *        </ul>
     * @see #captureMemorySnapshot()
     * @see #stopCPUProfiling()
     */
    public void startAllocationRecording(long mode) throws Exception;

    /**
     * @throws Exception if capture failed. The possible reasons are:
     *        <ul>
     *        <li>there's no Java application with properly configured profiler agent listening on port at host
     *        <li>profiled application has terminated
     *        <li>agent cannot capture snapshot for some reason
     *        </ul>
     */
    public void stopAllocationRecording() throws Exception;

    /**
     * Start CPU profiling.
     * @param mode YourKitProfilerServiceMBean.CPU_SAMPLING or YourKitProfilerServiceMBean.CPU_TRACING or
     *             YourKitProfilerServiceMBean.CPU_SAMPLING | YourKitProfilerServiceMBean.CPU_J2EE
     *             or YourKitProfilerServiceMBean.CPU_TRACING | YourKitProfilerServiceMBean.CPU_J2EE
     * @param filters string containing '\n'-separated list of classes whose methods should not be profiled.
     * Wildcards are accepted ('*'). More methods are profiled, bigger the overhead. The filters are used with
     * YourKitProfilerServiceMBean.CPU_TRACING only; with YourKitProfilerServiceMBean.CPU_SAMPLING the value is ignored.
     * If null or empty string passed, all methods will be profiled (not recommended due to high overhead).
     * For example, you can pass DEFAULT_FILTERS.
     * @throws Exception if capture failed. The possible reasons are:
     *        <ul>
     *        <li>there's no Java application with properly configured profiler agent listening on port at host
     *        <li>specified profiling mode is not supported by the JVM of the profiled application, e.g. tracing is
     *            supported with Java 5 and newer and thus is not available with Java 1.4.
     *        <li>profiled application has terminated
     *        <li>agent cannot capture snapshot for some reason
     *        </ul>
     * @see #captureSnapshot(long)
     * @see #stopCPUProfiling()
     */
    public void startCPUProfiling(long mode, String filters) throws Exception;

    /**
     * Stop CPU profiling.
     * @throws Exception if capture failed. The possible reasons are:
     *        <ul>
     *        <li>there's no Java application with properly configured profiler agent listening on port at host
     *        <li>profiled application has terminated
     *        <li>agent cannot capture snapshot for some reason
     *        </ul>
     * @see #captureSnapshot(long)
     * @see #startCPUProfiling(long , String )
     */
    public void stopCPUProfiling() throws Exception;

    /**
     * @return array of 2 elements: [0] - size of objects in heap before GC, bytes, [1] - size of objects in heap after GC, bytes
     */
    public long[] forceGC() throws Exception;

    /**
     * Start monitor profiling (requires that the profiled application runs on Java 5 or newer)
     * @throws Exception if capture failed. The possible reasons are:
     *        <ul>
     *        <li>there's no Java application with properly configured profiler agent listening on port at host
     *        <li>specified profiling mode is not supported by the JVM of the profiled application, e.g. tracing is
     *            supported with Java 5 and newer and thus is not available with Java 1.4.
     *        <li>CPU profiling has already been started
     *        <li>profiled application has terminated
     *        <li>agent cannot capture snapshot for some reason
     *        </ul>
     * @see #stopMonitorProfiling()
     * @see #captureSnapshot(long)
     */
    public void startMonitorProfiling() throws Exception;

    /**
     * Stop monitor profiling (requires that the profiled application runs on Java 5 or newer)
     * @throws Exception if capture failed. The possible reasons are:
     *        <ul>
     *        <li>there's no Java application with properly configured profiler agent listening on port at host
     *        <li>profiled application has terminated
     *        <li>agent cannot capture snapshot for some reason
     *        </ul>
     * @see #startMonitorProfiling()
     * @see #captureSnapshot(long)
     */
    public void stopMonitorProfiling() throws Exception;

    /**
     * Starts new daemon thread which calls {@link #captureMemorySnapshot()} every N seconds.
     * @param seconds delay between calls
     * @see #captureMemorySnapshot()
     */
    public void startCapturingMemorySnapshotEverySeconds(final int seconds);

    /**
     * Stops daemon thread started by {@link #startCapturingMemorySnapshotEverySeconds(int)}
     * @see #startCapturingMemorySnapshotEverySeconds(int)
     */
    public void stopCapturingMemorySnapshot();

    /**
     * Get current profiling status. The following code snippet demonstrates how to use this method:
     * <code><pre>
     * long status = controller.getStatus();
     *
     * if ((status & YourKitProfilerServiceMBean.ALLOCATION_RECORDING_ADAPTIVE) != 0) {
     *  System.out.println("Allocation recording is on (adaptive)");
     * }
     * else if ((status & YourKitProfilerServiceMBean.ALLOCATION_RECORDING_ALL) != 0) {
     *  System.out.println("Allocation recording is on (all objects)");
     * }
     * else {
     *  System.out.println("Allocation recording is off");
     * }
     *
     * if ((status & YourKitProfilerServiceMBean.CPU_TRACING) != 0) {
     *  System.out.println("CPU profiling is on (tracing)");
     * }
     * else if ((status & YourKitProfilerServiceMBean.CPU_SAMPLING) != 0) {
     *  System.out.println("CPU profiling is on (sampling)");
     * }
     * else {
     *  System.out.println("CPU profiling is off");
     * }
     *
     * if ((status & YourKitProfilerServiceMBean.MONITOR_PROFILING) != 0) {
     *  System.out.println("Monitor profiling is on");
     * }
     * else {
     *  System.out.println("Monitor profiling is off");
     * }</pre></code>
     * @return a bit mask to check against Profiling Modes
     * @throws java.lang.Exception
     */
    public long getStatus() throws java.lang.Exception;

}
