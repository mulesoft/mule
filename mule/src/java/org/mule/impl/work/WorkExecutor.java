/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.mule.impl.work;

import javax.resource.spi.work.WorkException;

import EDU.oswego.cs.dl.util.concurrent.Executor;

/**
 * <code>WorkExecutor</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface WorkExecutor
{

    /**
     * This method must be implemented by sub-classes in order to provide the
     * relevant synchronization policy. It is called by the executeWork template
     * method.
     * 
     * @param work Work to be executed.
     * 
     * @throws javax.resource.spi.work.WorkException Indicates that the work has
     *             failed.
     * @throws InterruptedException Indicates that the thread in charge of the
     *             execution of the specified work has been interrupted.
     */
    void doExecute(WorkerContext work, Executor executor) throws WorkException, InterruptedException;

}
