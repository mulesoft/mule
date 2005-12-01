/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.ConnectionStrategyType;
import org.mule.schema.MuleEnvironmentPropertiesType;
import org.mule.schema.PersistenceStrategyType;
import org.mule.schema.PoolingProfileType;
import org.mule.schema.QueueProfileType;
import org.mule.schema.SchemaPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Mule Environment Properties Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getThreadingProfile <em>Threading Profile</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getPoolingProfile <em>Pooling Profile</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getQueueProfile <em>Queue Profile</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getPersistenceStrategy <em>Persistence Strategy</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getConnectionStrategy <em>Connection Strategy</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isClientMode <em>Client Mode</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getModel <em>Model</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isRecoverableMode <em>Recoverable Mode</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getServerUrl <em>Server Url</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getSynchronousEventTimeout <em>Synchronous Event Timeout</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isSynchronousReceive <em>Synchronous Receive</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getTransactionTimeout <em>Transaction Timeout</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getWorkingDirectory <em>Working Directory</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MuleEnvironmentPropertiesTypeImpl extends EObjectImpl implements MuleEnvironmentPropertiesType {
	/**
	 * The cached value of the '{@link #getMixed() <em>Mixed</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMixed()
	 * @generated
	 * @ordered
	 */
	protected FeatureMap mixed = null;

	/**
	 * The default value of the '{@link #isClientMode() <em>Client Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isClientMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean CLIENT_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isClientMode() <em>Client Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isClientMode()
	 * @generated
	 * @ordered
	 */
	protected boolean clientMode = CLIENT_MODE_EDEFAULT;

	/**
	 * This is true if the Client Mode attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean clientModeESet = false;

	/**
	 * The default value of the '{@link #getModel() <em>Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getModel()
	 * @generated
	 * @ordered
	 */
	protected static final String MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getModel() <em>Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getModel()
	 * @generated
	 * @ordered
	 */
	protected String model = MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #isRecoverableMode() <em>Recoverable Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRecoverableMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean RECOVERABLE_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isRecoverableMode() <em>Recoverable Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRecoverableMode()
	 * @generated
	 * @ordered
	 */
	protected boolean recoverableMode = RECOVERABLE_MODE_EDEFAULT;

	/**
	 * This is true if the Recoverable Mode attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean recoverableModeESet = false;

	/**
	 * The default value of the '{@link #getServerUrl() <em>Server Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getServerUrl()
	 * @generated
	 * @ordered
	 */
	protected static final String SERVER_URL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getServerUrl() <em>Server Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getServerUrl()
	 * @generated
	 * @ordered
	 */
	protected String serverUrl = SERVER_URL_EDEFAULT;

	/**
	 * The default value of the '{@link #isSynchronous() <em>Synchronous</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSynchronous()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SYNCHRONOUS_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSynchronous() <em>Synchronous</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSynchronous()
	 * @generated
	 * @ordered
	 */
	protected boolean synchronous = SYNCHRONOUS_EDEFAULT;

	/**
	 * This is true if the Synchronous attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean synchronousESet = false;

	/**
	 * The default value of the '{@link #getSynchronousEventTimeout() <em>Synchronous Event Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSynchronousEventTimeout()
	 * @generated
	 * @ordered
	 */
	protected static final String SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSynchronousEventTimeout() <em>Synchronous Event Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSynchronousEventTimeout()
	 * @generated
	 * @ordered
	 */
	protected String synchronousEventTimeout = SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT;

	/**
	 * The default value of the '{@link #isSynchronousReceive() <em>Synchronous Receive</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSynchronousReceive()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SYNCHRONOUS_RECEIVE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSynchronousReceive() <em>Synchronous Receive</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSynchronousReceive()
	 * @generated
	 * @ordered
	 */
	protected boolean synchronousReceive = SYNCHRONOUS_RECEIVE_EDEFAULT;

	/**
	 * This is true if the Synchronous Receive attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean synchronousReceiveESet = false;

	/**
	 * The default value of the '{@link #getTransactionTimeout() <em>Transaction Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTransactionTimeout()
	 * @generated
	 * @ordered
	 */
	protected static final String TRANSACTION_TIMEOUT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTransactionTimeout() <em>Transaction Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTransactionTimeout()
	 * @generated
	 * @ordered
	 */
	protected String transactionTimeout = TRANSACTION_TIMEOUT_EDEFAULT;

	/**
	 * The default value of the '{@link #getWorkingDirectory() <em>Working Directory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWorkingDirectory()
	 * @generated
	 * @ordered
	 */
	protected static final String WORKING_DIRECTORY_EDEFAULT = "./.mule";

	/**
	 * The cached value of the '{@link #getWorkingDirectory() <em>Working Directory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWorkingDirectory()
	 * @generated
	 * @ordered
	 */
	protected String workingDirectory = WORKING_DIRECTORY_EDEFAULT;

	/**
	 * This is true if the Working Directory attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean workingDirectoryESet = false;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MuleEnvironmentPropertiesTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getThreadingProfile() {
		return ((FeatureMap)getMixed()).list(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_ThreadingProfile());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PoolingProfileType getPoolingProfile() {
		return (PoolingProfileType)getMixed().get(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_PoolingProfile(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPoolingProfile(PoolingProfileType newPoolingProfile, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_PoolingProfile(), newPoolingProfile, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPoolingProfile(PoolingProfileType newPoolingProfile) {
		((FeatureMap.Internal)getMixed()).set(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_PoolingProfile(), newPoolingProfile);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public QueueProfileType getQueueProfile() {
		return (QueueProfileType)getMixed().get(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_QueueProfile(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetQueueProfile(QueueProfileType newQueueProfile, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_QueueProfile(), newQueueProfile, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setQueueProfile(QueueProfileType newQueueProfile) {
		((FeatureMap.Internal)getMixed()).set(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_QueueProfile(), newQueueProfile);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PersistenceStrategyType getPersistenceStrategy() {
		return (PersistenceStrategyType)getMixed().get(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_PersistenceStrategy(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPersistenceStrategy(PersistenceStrategyType newPersistenceStrategy, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_PersistenceStrategy(), newPersistenceStrategy, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPersistenceStrategy(PersistenceStrategyType newPersistenceStrategy) {
		((FeatureMap.Internal)getMixed()).set(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_PersistenceStrategy(), newPersistenceStrategy);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConnectionStrategyType getConnectionStrategy() {
		return (ConnectionStrategyType)getMixed().get(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_ConnectionStrategy(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetConnectionStrategy(ConnectionStrategyType newConnectionStrategy, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_ConnectionStrategy(), newConnectionStrategy, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setConnectionStrategy(ConnectionStrategyType newConnectionStrategy) {
		((FeatureMap.Internal)getMixed()).set(SchemaPackage.eINSTANCE.getMuleEnvironmentPropertiesType_ConnectionStrategy(), newConnectionStrategy);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isClientMode() {
		return clientMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setClientMode(boolean newClientMode) {
		boolean oldClientMode = clientMode;
		clientMode = newClientMode;
		boolean oldClientModeESet = clientModeESet;
		clientModeESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE, oldClientMode, clientMode, !oldClientModeESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetClientMode() {
		boolean oldClientMode = clientMode;
		boolean oldClientModeESet = clientModeESet;
		clientMode = CLIENT_MODE_EDEFAULT;
		clientModeESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE, oldClientMode, CLIENT_MODE_EDEFAULT, oldClientModeESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetClientMode() {
		return clientModeESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getModel() {
		return model;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setModel(String newModel) {
		String oldModel = model;
		model = newModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL, oldModel, model));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isRecoverableMode() {
		return recoverableMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRecoverableMode(boolean newRecoverableMode) {
		boolean oldRecoverableMode = recoverableMode;
		recoverableMode = newRecoverableMode;
		boolean oldRecoverableModeESet = recoverableModeESet;
		recoverableModeESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE, oldRecoverableMode, recoverableMode, !oldRecoverableModeESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetRecoverableMode() {
		boolean oldRecoverableMode = recoverableMode;
		boolean oldRecoverableModeESet = recoverableModeESet;
		recoverableMode = RECOVERABLE_MODE_EDEFAULT;
		recoverableModeESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE, oldRecoverableMode, RECOVERABLE_MODE_EDEFAULT, oldRecoverableModeESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetRecoverableMode() {
		return recoverableModeESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getServerUrl() {
		return serverUrl;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setServerUrl(String newServerUrl) {
		String oldServerUrl = serverUrl;
		serverUrl = newServerUrl;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL, oldServerUrl, serverUrl));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSynchronous() {
		return synchronous;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSynchronous(boolean newSynchronous) {
		boolean oldSynchronous = synchronous;
		synchronous = newSynchronous;
		boolean oldSynchronousESet = synchronousESet;
		synchronousESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS, oldSynchronous, synchronous, !oldSynchronousESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetSynchronous() {
		boolean oldSynchronous = synchronous;
		boolean oldSynchronousESet = synchronousESet;
		synchronous = SYNCHRONOUS_EDEFAULT;
		synchronousESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS, oldSynchronous, SYNCHRONOUS_EDEFAULT, oldSynchronousESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetSynchronous() {
		return synchronousESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getSynchronousEventTimeout() {
		return synchronousEventTimeout;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSynchronousEventTimeout(String newSynchronousEventTimeout) {
		String oldSynchronousEventTimeout = synchronousEventTimeout;
		synchronousEventTimeout = newSynchronousEventTimeout;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT, oldSynchronousEventTimeout, synchronousEventTimeout));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSynchronousReceive() {
		return synchronousReceive;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSynchronousReceive(boolean newSynchronousReceive) {
		boolean oldSynchronousReceive = synchronousReceive;
		synchronousReceive = newSynchronousReceive;
		boolean oldSynchronousReceiveESet = synchronousReceiveESet;
		synchronousReceiveESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_RECEIVE, oldSynchronousReceive, synchronousReceive, !oldSynchronousReceiveESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetSynchronousReceive() {
		boolean oldSynchronousReceive = synchronousReceive;
		boolean oldSynchronousReceiveESet = synchronousReceiveESet;
		synchronousReceive = SYNCHRONOUS_RECEIVE_EDEFAULT;
		synchronousReceiveESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_RECEIVE, oldSynchronousReceive, SYNCHRONOUS_RECEIVE_EDEFAULT, oldSynchronousReceiveESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetSynchronousReceive() {
		return synchronousReceiveESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getTransactionTimeout() {
		return transactionTimeout;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTransactionTimeout(String newTransactionTimeout) {
		String oldTransactionTimeout = transactionTimeout;
		transactionTimeout = newTransactionTimeout;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT, oldTransactionTimeout, transactionTimeout));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setWorkingDirectory(String newWorkingDirectory) {
		String oldWorkingDirectory = workingDirectory;
		workingDirectory = newWorkingDirectory;
		boolean oldWorkingDirectoryESet = workingDirectoryESet;
		workingDirectoryESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY, oldWorkingDirectory, workingDirectory, !oldWorkingDirectoryESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetWorkingDirectory() {
		String oldWorkingDirectory = workingDirectory;
		boolean oldWorkingDirectoryESet = workingDirectoryESet;
		workingDirectory = WORKING_DIRECTORY_EDEFAULT;
		workingDirectoryESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY, oldWorkingDirectory, WORKING_DIRECTORY_EDEFAULT, oldWorkingDirectoryESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetWorkingDirectory() {
		return workingDirectoryESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
					return ((InternalEList)getThreadingProfile()).basicRemove(otherEnd, msgs);
				case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
					return basicSetPoolingProfile(null, msgs);
				case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
					return basicSetQueueProfile(null, msgs);
				case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
					return basicSetPersistenceStrategy(null, msgs);
				case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
					return basicSetConnectionStrategy(null, msgs);
				default:
					return eDynamicInverseRemove(otherEnd, featureID, baseClass, msgs);
			}
		}
		return eBasicSetContainer(null, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object eGet(EStructuralFeature eFeature, boolean resolve) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				return getMixed();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				return getThreadingProfile();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				return getPoolingProfile();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				return getQueueProfile();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				return getPersistenceStrategy();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				return getConnectionStrategy();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				return isClientMode() ? Boolean.TRUE : Boolean.FALSE;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				return getModel();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				return isRecoverableMode() ? Boolean.TRUE : Boolean.FALSE;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				return getServerUrl();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				return isSynchronous() ? Boolean.TRUE : Boolean.FALSE;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				return getSynchronousEventTimeout();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_RECEIVE:
				return isSynchronousReceive() ? Boolean.TRUE : Boolean.FALSE;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				return getTransactionTimeout();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
				return getWorkingDirectory();
		}
		return eDynamicGet(eFeature, resolve);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void eSet(EStructuralFeature eFeature, Object newValue) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				getThreadingProfile().clear();
				getThreadingProfile().addAll((Collection)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				setPoolingProfile((PoolingProfileType)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				setQueueProfile((QueueProfileType)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				setPersistenceStrategy((PersistenceStrategyType)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				setConnectionStrategy((ConnectionStrategyType)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				setClientMode(((Boolean)newValue).booleanValue());
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				setModel((String)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				setRecoverableMode(((Boolean)newValue).booleanValue());
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				setServerUrl((String)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				setSynchronous(((Boolean)newValue).booleanValue());
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				setSynchronousEventTimeout((String)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_RECEIVE:
				setSynchronousReceive(((Boolean)newValue).booleanValue());
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				setTransactionTimeout((String)newValue);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
				setWorkingDirectory((String)newValue);
				return;
		}
		eDynamicSet(eFeature, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void eUnset(EStructuralFeature eFeature) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				getMixed().clear();
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				getThreadingProfile().clear();
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				setPoolingProfile((PoolingProfileType)null);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				setQueueProfile((QueueProfileType)null);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				setPersistenceStrategy((PersistenceStrategyType)null);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				setConnectionStrategy((ConnectionStrategyType)null);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				unsetClientMode();
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				setModel(MODEL_EDEFAULT);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				unsetRecoverableMode();
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				setServerUrl(SERVER_URL_EDEFAULT);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				unsetSynchronous();
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				setSynchronousEventTimeout(SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_RECEIVE:
				unsetSynchronousReceive();
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				setTransactionTimeout(TRANSACTION_TIMEOUT_EDEFAULT);
				return;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
				unsetWorkingDirectory();
				return;
		}
		eDynamicUnset(eFeature);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean eIsSet(EStructuralFeature eFeature) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				return !getThreadingProfile().isEmpty();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				return getPoolingProfile() != null;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				return getQueueProfile() != null;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				return getPersistenceStrategy() != null;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				return getConnectionStrategy() != null;
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				return isSetClientMode();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				return MODEL_EDEFAULT == null ? model != null : !MODEL_EDEFAULT.equals(model);
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				return isSetRecoverableMode();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				return SERVER_URL_EDEFAULT == null ? serverUrl != null : !SERVER_URL_EDEFAULT.equals(serverUrl);
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				return isSetSynchronous();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				return SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT == null ? synchronousEventTimeout != null : !SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT.equals(synchronousEventTimeout);
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_RECEIVE:
				return isSetSynchronousReceive();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				return TRANSACTION_TIMEOUT_EDEFAULT == null ? transactionTimeout != null : !TRANSACTION_TIMEOUT_EDEFAULT.equals(transactionTimeout);
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
				return isSetWorkingDirectory();
		}
		return eDynamicIsSet(eFeature);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (mixed: ");
		result.append(mixed);
		result.append(", clientMode: ");
		if (clientModeESet) result.append(clientMode); else result.append("<unset>");
		result.append(", model: ");
		result.append(model);
		result.append(", recoverableMode: ");
		if (recoverableModeESet) result.append(recoverableMode); else result.append("<unset>");
		result.append(", serverUrl: ");
		result.append(serverUrl);
		result.append(", synchronous: ");
		if (synchronousESet) result.append(synchronous); else result.append("<unset>");
		result.append(", synchronousEventTimeout: ");
		result.append(synchronousEventTimeout);
		result.append(", synchronousReceive: ");
		if (synchronousReceiveESet) result.append(synchronousReceive); else result.append("<unset>");
		result.append(", transactionTimeout: ");
		result.append(transactionTimeout);
		result.append(", workingDirectory: ");
		if (workingDirectoryESet) result.append(workingDirectory); else result.append("<unset>");
		result.append(')');
		return result.toString();
	}

} //MuleEnvironmentPropertiesTypeImpl
