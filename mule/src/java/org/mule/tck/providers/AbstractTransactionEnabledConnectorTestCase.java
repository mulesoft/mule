/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 08-Feb-2004
 * Time: 17:38:56
 */
package org.mule.tck.providers;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.mule.impl.MuleComponent;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.providers.TransactionEnabledConnector;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

public abstract class AbstractTransactionEnabledConnectorTestCase extends AbstractConnectorTestCase
{

    TestDispatcher testDispatcher;

    /*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
    protected void setUp() throws Exception
    {
        super.setUp();
        testDispatcher = new TestDispatcher();
    }

    public void testDispatchWithNoTransactionAndAlwaysCommit() throws Exception
    {
        UMOConnector connector = getConnector();

        UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);

        endpoint.getTransactionConfig().setCommitAction(UMOTransactionConfig.ACTION_ALWAYS_COMMIT);
        UMOEvent event = getTestEvent(getValidMessage(), endpoint);

        connector.setDispatcherFactory(new UMOMessageDispatcherFactory()
        {
            public UMOMessageDispatcher create(UMOConnector connector)
            {
                return testDispatcher;
            }
        });

        connector.getDispatcher("dummy").dispatch(event);
        assertEquals(event, ((TestDispatcher) connector.getDispatcher("dummy")).getEvent());
        try
        {
            ((TransactionEnabledConnector) connector).commitTransaction(event);
            fail("There is no transaction active to commit");
        }
        catch (UMOTransactionException e)
        {
            //expected
        }
    }

    public void testDispatchWithTransactionAndAlwaysCommit() throws Exception
    {
        doTestDispatchWithTransaction(UMOTransactionConfig.ACTION_ALWAYS_COMMIT);
    }

    public void testDispatchWithTransactionAndCommitIfPossible() throws Exception
    {
        doTestDispatchWithTransaction(UMOTransactionConfig.ACTION_COMMIT_IF_POSSIBLE);
    }

    public void testDispatchWithNoTransaction() throws Exception
    {
        doTestDispatchWithNoTransaction(UMOTransactionConfig.ACTION_NONE);
    }

    public void testDispatchWithNoTransactionAndCommitIfPossible() throws Exception
    {
        doTestDispatchWithNoTransaction(UMOTransactionConfig.ACTION_COMMIT_IF_POSSIBLE);
    }

    public void doTestDispatchWithTransaction(byte action) throws Exception
    {
        MuleComponent component = getTestComponent(descriptor);
        Mock transaction = getMockTransaction();
        
        UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        UMOSession session = new MuleSession(component, (UMOTransaction) transaction.proxy());
        UMOEvent event = new MuleEvent(new MuleMessage(getValidMessage(), null), endpoint, session, true);

        UMOConnector connector = getConnector();
        endpoint.getTransactionConfig().setCommitAction(action);

        connector.setDispatcherFactory(new UMOMessageDispatcherFactory()
        {
            public UMOMessageDispatcher create(UMOConnector connector)
            {
                return testDispatcher;
            }
        });

        connector.getDispatcher("dummy").dispatch(event);
        assertEquals(event, ((TestDispatcher) connector.getDispatcher("dummy")).getEvent());
        transaction.expectAndReturn("isRollbackOnly", false);
        transaction.expectAndReturn("isRollbackOnly", false);
        transaction.expectAndReturn("isRollbackOnly", false);
        transaction.expectAndReturn("isCommitted", false);
        transaction.expect("commit");
        TransactionCoordination.getInstance().bindTransaction((UMOTransaction) transaction.proxy(), null);

        //As we are using a mock connectorSession we call commit here to make sure the correct behaviour
        //is executed
        ((TransactionEnabledConnector) connector).commitTransaction(event);

        transaction.verify();
    }

    public void doTestDispatchWithNoTransaction(byte action) throws Exception
    {
        UMOConnector connector = getConnector();
        UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);

        endpoint.getTransactionConfig().setCommitAction(action);
        UMOEvent event = getTestEvent(getValidMessage(), endpoint);
        connector.setDispatcherFactory(new UMOMessageDispatcherFactory()
        {
            public UMOMessageDispatcher create(UMOConnector connector)
            {
                return testDispatcher;
            }
        });

        connector.getDispatcher("dummy").dispatch(event);
        assertEquals(event, ((TestDispatcher) connector.getDispatcher("dummy")).getEvent());
        //As we are using a mock session we call commit here to make sure the correct behaviour
        //is executed
        ((TransactionEnabledConnector) connector).commitTransaction(event);
    }

    public void testBeginWithNoTransaction() throws Exception
    {
        UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);

        endpoint.getTransactionConfig().setBeginAction(UMOTransactionConfig.ACTION_NONE);
        UMOTransaction trans = getTxConnector().beginTransaction(endpoint);
        assertNull(trans);
    }

    public void testBeginWithNoTransactionAndBeginOrJoin() throws Exception
    {
        doTestBeginWithNoTransactionAndBegin(UMOTransactionConfig.ACTION_BEGIN_OR_JOIN);
    }

    public void testBeginWithNoTransactionAndAlwaysBegin() throws Exception
    {
        doTestBeginWithNoTransactionAndBegin(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
    }

    public void testBeginWithNoTransactionAndJoinIfPossible() throws Exception
    {
        //Only supported for XA
        UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint.getTransactionConfig().setBeginAction(UMOTransactionConfig.ACTION_JOIN_IF_POSSIBLE);

        UMOTransaction trans = getTxConnector().beginTransaction(endpoint);
        assertNull(trans);
    }

    public void doTestBeginWithNoTransactionAndBegin(byte action) throws Exception
    {
        UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        Mock txFactory = getMockTransactionFactory();
        Mock transaction = getMockTransaction();

        endpoint.getTransactionConfig().setBeginAction(action);
        endpoint.getTransactionConfig().setFactory((UMOTransactionFactory) txFactory.proxy());
        txFactory.expectAndReturn("beginTransaction", C.IS_NOT_NULL, (UMOTransaction) transaction.proxy());
        txFactory.expectAndReturn("isTransacted", true);
        UMOTransaction trans = getTxConnector().beginTransaction(endpoint);

        txFactory.verify();
        assertNotNull(trans);
    }

    public void doTestBeginWithTransactionAndAlwaysBegin() throws Exception
    {
        UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        Mock transaction = getMockTransaction();
        TransactionCoordination.getInstance().bindTransaction((UMOTransaction) transaction.proxy(), null);

        endpoint.getTransactionConfig().setBeginAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        try
        {
            getTxConnector().beginTransaction(endpoint);
            fail("Should fail as a transaction is already bound");
        }
        catch (UMOTransactionException e)
        {
            //expected
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction();
        }
    }

    public void doTestBeginWithTransactionAndBeginOrJoin() throws Exception
    {
        try
        {
            UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
            Mock transaction = getMockTransaction();
            transaction.expect("begin");
            TransactionCoordination.getInstance().bindTransaction((UMOTransaction) transaction.proxy(), null);

            endpoint.getTransactionConfig().setBeginAction(UMOTransactionConfig.ACTION_BEGIN_OR_JOIN);
            UMOTransaction trans = getTxConnector().beginTransaction(endpoint);
            transaction.verify();

            assertEquals(transaction, trans);
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction();
        }
    }

    public void doTestBeginWithTransactionAndBJoinIfPossible() throws Exception
    {
        try
        {
            UMOEndpoint endpoint = getTestEndpoint("test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
            Mock transaction = getMockTransaction();
            TransactionCoordination.getInstance().bindTransaction((UMOTransaction) transaction.proxy(), null);

            endpoint.getTransactionConfig().setBeginAction(UMOTransactionConfig.ACTION_JOIN_IF_POSSIBLE);
            UMOTransaction trans = getTxConnector().beginTransaction(endpoint);
            transaction.verify();

            assertEquals(transaction, trans);
        }
        finally
        {
            TransactionCoordination.getInstance().unbindTransaction();
        }
    }

    public TransactionEnabledConnector getTxConnector() throws Exception
    {
        return (TransactionEnabledConnector) getConnector();
    }

    private class TestDispatcher implements UMOMessageDispatcher
    {
        private UMOEvent event;

        public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
        {
            return null;
        }

        public void dispatch(UMOEvent event) throws Exception
        {
            this.event = event;
        }

        public UMOConnector getConnector()
        {
            return getConnector();
        }

        public Object getDelegateSession() throws UMOException
        {
            //return null;
            throw new UnsupportedOperationException("delegate session not implemented");
        }

        public UMOMessage send(UMOEvent event) throws Exception
        {
            this.event = event;
            return event.getMessage();
        }

        public void run()
        {
            throw new UnsupportedOperationException("Run is not implemented");
        }

        public UMOEvent getEvent()
        {
            return event;
        }

        public void dispose() throws UMOException
        {
        }

        public boolean isDisposed()
        {
            return false;
        }
    }

    protected void tearDown() throws Exception
    {
        TransactionCoordination.getInstance().unbindTransaction();
    }
}