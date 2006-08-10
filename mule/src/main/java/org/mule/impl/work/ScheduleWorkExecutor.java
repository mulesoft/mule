/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mule.impl.work;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

import javax.resource.spi.work.WorkException;

/**
 * 
 * 
 * @version $Rev$ $Date$
 * 
 */
public class ScheduleWorkExecutor implements WorkExecutor
{

    public void doExecute(WorkerContext work, Executor executor) throws WorkException, InterruptedException
    {
        executor.execute(work);
    }
}
