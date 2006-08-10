/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or =mplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.test.integration.providers.soap.axis.style;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.SOAPEnvelope;

/**
 * Simple message-style service sample.
 */

public interface MessageService {
    /**
     * Service methods, echo back any XML received.
     *
     */
    
   public org.apache.axis.message.SOAPBodyElement [] soapBodyElement(org.apache.axis.message.SOAPBodyElement [] bodyElements);

    public Document document(Document body);

    public Element[] elementArray(Element [] elems);

    public void soapRequestResponse(SOAPEnvelope req, SOAPEnvelope resp) throws javax.xml.soap.SOAPException;

}