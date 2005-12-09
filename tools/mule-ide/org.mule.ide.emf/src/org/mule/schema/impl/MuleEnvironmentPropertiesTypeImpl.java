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
import org.mule.schema.MulePackage;
import org.mule.schema.PersistenceStrategyType;
import org.mule.schema.PoolingProfileType;
import org.mule.schema.QueueProfileType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Environment Properties Type</b></em>'.
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
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isEmbedded <em>Embedded</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isEnableMessageEvents <em>Enable Message Events</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getEncoding <em>Encoding</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getModel <em>Model</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isRecoverableMode <em>Recoverable Mode</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isRemoteSync <em>Remote Sync</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getServerUrl <em>Server Url</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl#getSynchronousEventTimeout <em>Synchronous Event Timeout</em>}</li>
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
	 * The default value of the '{@link #isEmbedded() <em>Embedded</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isEmbedded()
	 * @generated
	 * @ordered
	 */
	protected static final boolean EMBEDDED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isEmbedded() <em>Embedded</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isEmbedded()
	 * @generated
	 * @ordered
	 */
	protected boolean embedded = EMBEDDED_EDEFAULT;

	/**
	 * This is true if the Embedded attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean embeddedESet = false;

	/**
	 * The default value of the '{@link #isEnableMessageEvents() <em>Enable Message Events</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isEnableMessageEvents()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ENABLE_MESSAGE_EVENTS_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isEnableMessageEvents() <em>Enable Message Events</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isEnableMessageEvents()
	 * @generated
	 * @ordered
	 */
	protected boolean enableMessageEvents = ENABLE_MESSAGE_EVENTS_EDEFAULT;

	/**
	 * This is true if the Enable Message Events attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean enableMessageEventsESet = false;

	/**
	 * The default value of the '{@link #getEncoding() <em>Encoding</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEncoding()
	 * @generated
	 * @ordered
	 */
	protected static final String ENCODING_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getEncoding() <em>Encoding</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEncoding()
	 * @generated
	 * @ordered
	 */
	protected String encoding = ENCODING_EDEFAULT;

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
	 * The default value of the '{@link #isRemoteSync() <em>Remote Sync</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRemoteSync()
	 * @generated
	 * @ordered
	 */
	protected static final boolean REMOTE_SYNC_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isRemoteSync() <em>Remote Sync</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRemoteSync()
	 * @generated
	 * @ordered
	 */
	protected boolean remoteSync = REMOTE_SYNC_EDEFAULT;

	/**
	 * This is true if the Remote Sync attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean remoteSyncESet = false;

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
		return MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getThreadingProfile() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_ThreadingProfile());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PoolingProfileType getPoolingProfile() {
		return (PoolingProfileType)getMixed().get(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_PoolingProfile(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPoolingProfile(PoolingProfileType newPoolingProfile, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_PoolingProfile(), newPoolingProfile, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPoolingProfile(PoolingProfileType newPoolingProfile) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_PoolingProfile(), newPoolingProfile);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public QueueProfileType getQueueProfile() {
		return (QueueProfileType)getMixed().get(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_QueueProfile(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetQueueProfile(QueueProfileType newQueueProfile, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_QueueProfile(), newQueueProfile, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setQueueProfile(QueueProfileType newQueueProfile) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_QueueProfile(), newQueueProfile);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PersistenceStrategyType getPersistenceStrategy() {
		return (PersistenceStrategyType)getMixed().get(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_PersistenceStrategy(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPersistenceStrategy(PersistenceStrategyType newPersistenceStrategy, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_PersistenceStrategy(), newPersistenceStrategy, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPersistenceStrategy(PersistenceStrategyType newPersistenceStrategy) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_PersistenceStrategy(), newPersistenceStrategy);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConnectionStrategyType getConnectionStrategy() {
		return (ConnectionStrategyType)getMixed().get(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_ConnectionStrategy(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetConnectionStrategy(ConnectionStrategyType newConnectionStrategy, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_ConnectionStrategy(), newConnectionStrategy, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setConnectionStrategy(ConnectionStrategyType newConnectionStrategy) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleEnvironmentPropertiesType_ConnectionStrategy(), newConnectionStrategy);
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE, oldClientMode, clientMode, !oldClientModeESet));
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
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE, oldClientMode, CLIENT_MODE_EDEFAULT, oldClientModeESet));
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
	public boolean isEmbedded() {
		return embedded;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEmbedded(boolean newEmbedded) {
		boolean oldEmbedded = embedded;
		embedded = newEmbedded;
		boolean oldEmbeddedESet = embeddedESet;
		embeddedESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED, oldEmbedded, embedded, !oldEmbeddedESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetEmbedded() {
		boolean oldEmbedded = embedded;
		boolean oldEmbeddedESet = embeddedESet;
		embedded = EMBEDDED_EDEFAULT;
		embeddedESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED, oldEmbedded, EMBEDDED_EDEFAULT, oldEmbeddedESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetEmbedded() {
		return embeddedESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isEnableMessageEvents() {
		return enableMessageEvents;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEnableMessageEvents(boolean newEnableMessageEvents) {
		boolean oldEnableMessageEvents = enableMessageEvents;
		enableMessageEvents = newEnableMessageEvents;
		boolean oldEnableMessageEventsESet = enableMessageEventsESet;
		enableMessageEventsESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS, oldEnableMessageEvents, enableMessageEvents, !oldEnableMessageEventsESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetEnableMessageEvents() {
		boolean oldEnableMessageEvents = enableMessageEvents;
		boolean oldEnableMessageEventsESet = enableMessageEventsESet;
		enableMessageEvents = ENABLE_MESSAGE_EVENTS_EDEFAULT;
		enableMessageEventsESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS, oldEnableMessageEvents, ENABLE_MESSAGE_EVENTS_EDEFAULT, oldEnableMessageEventsESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetEnableMessageEvents() {
		return enableMessageEventsESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEncoding(String newEncoding) {
		String oldEncoding = encoding;
		encoding = newEncoding;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENCODING, oldEncoding, encoding));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL, oldModel, model));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE, oldRecoverableMode, recoverableMode, !oldRecoverableModeESet));
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
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE, oldRecoverableMode, RECOVERABLE_MODE_EDEFAULT, oldRecoverableModeESet));
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
	public boolean isRemoteSync() {
		return remoteSync;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRemoteSync(boolean newRemoteSync) {
		boolean oldRemoteSync = remoteSync;
		remoteSync = newRemoteSync;
		boolean oldRemoteSyncESet = remoteSyncESet;
		remoteSyncESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC, oldRemoteSync, remoteSync, !oldRemoteSyncESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetRemoteSync() {
		boolean oldRemoteSync = remoteSync;
		boolean oldRemoteSyncESet = remoteSyncESet;
		remoteSync = REMOTE_SYNC_EDEFAULT;
		remoteSyncESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC, oldRemoteSync, REMOTE_SYNC_EDEFAULT, oldRemoteSyncESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetRemoteSync() {
		return remoteSyncESet;
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL, oldServerUrl, serverUrl));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS, oldSynchronous, synchronous, !oldSynchronousESet));
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
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS, oldSynchronous, SYNCHRONOUS_EDEFAULT, oldSynchronousESet));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT, oldSynchronousEventTimeout, synchronousEventTimeout));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT, oldTransactionTimeout, transactionTimeout));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY, oldWorkingDirectory, workingDirectory, !oldWorkingDirectoryESet));
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
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY, oldWorkingDirectory, WORKING_DIRECTORY_EDEFAULT, oldWorkingDirectoryESet));
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
				case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
					return ((InternalEList)getThreadingProfile()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
					return basicSetPoolingProfile(null, msgs);
				case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
					return basicSetQueueProfile(null, msgs);
				case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
					return basicSetPersistenceStrategy(null, msgs);
				case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
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
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				return getMixed();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				return getThreadingProfile();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				return getPoolingProfile();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				return getQueueProfile();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				return getPersistenceStrategy();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				return getConnectionStrategy();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				return isClientMode() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED:
				return isEmbedded() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS:
				return isEnableMessageEvents() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENCODING:
				return getEncoding();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				return getModel();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				return isRecoverableMode() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC:
				return isRemoteSync() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				return getServerUrl();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				return isSynchronous() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				return getSynchronousEventTimeout();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				return getTransactionTimeout();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
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
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				getThreadingProfile().clear();
				getThreadingProfile().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				setPoolingProfile((PoolingProfileType)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				setQueueProfile((QueueProfileType)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				setPersistenceStrategy((PersistenceStrategyType)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				setConnectionStrategy((ConnectionStrategyType)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				setClientMode(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED:
				setEmbedded(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS:
				setEnableMessageEvents(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENCODING:
				setEncoding((String)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				setModel((String)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				setRecoverableMode(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC:
				setRemoteSync(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				setServerUrl((String)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				setSynchronous(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				setSynchronousEventTimeout((String)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				setTransactionTimeout((String)newValue);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
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
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				getThreadingProfile().clear();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				setPoolingProfile((PoolingProfileType)null);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				setQueueProfile((QueueProfileType)null);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				setPersistenceStrategy((PersistenceStrategyType)null);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				setConnectionStrategy((ConnectionStrategyType)null);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				unsetClientMode();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED:
				unsetEmbedded();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS:
				unsetEnableMessageEvents();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENCODING:
				setEncoding(ENCODING_EDEFAULT);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				setModel(MODEL_EDEFAULT);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				unsetRecoverableMode();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC:
				unsetRemoteSync();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				setServerUrl(SERVER_URL_EDEFAULT);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				unsetSynchronous();
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				setSynchronousEventTimeout(SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				setTransactionTimeout(TRANSACTION_TIMEOUT_EDEFAULT);
				return;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
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
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE:
				return !getThreadingProfile().isEmpty();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE:
				return getPoolingProfile() != null;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE:
				return getQueueProfile() != null;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY:
				return getPersistenceStrategy() != null;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY:
				return getConnectionStrategy() != null;
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE:
				return isSetClientMode();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED:
				return isSetEmbedded();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS:
				return isSetEnableMessageEvents();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__ENCODING:
				return ENCODING_EDEFAULT == null ? encoding != null : !ENCODING_EDEFAULT.equals(encoding);
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL:
				return MODEL_EDEFAULT == null ? model != null : !MODEL_EDEFAULT.equals(model);
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE:
				return isSetRecoverableMode();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC:
				return isSetRemoteSync();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL:
				return SERVER_URL_EDEFAULT == null ? serverUrl != null : !SERVER_URL_EDEFAULT.equals(serverUrl);
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS:
				return isSetSynchronous();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT:
				return SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT == null ? synchronousEventTimeout != null : !SYNCHRONOUS_EVENT_TIMEOUT_EDEFAULT.equals(synchronousEventTimeout);
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT:
				return TRANSACTION_TIMEOUT_EDEFAULT == null ? transactionTimeout != null : !TRANSACTION_TIMEOUT_EDEFAULT.equals(transactionTimeout);
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY:
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
		result.append(", embedded: ");
		if (embeddedESet) result.append(embedded); else result.append("<unset>");
		result.append(", enableMessageEvents: ");
		if (enableMessageEventsESet) result.append(enableMessageEvents); else result.append("<unset>");
		result.append(", encoding: ");
		result.append(encoding);
		result.append(", model: ");
		result.append(model);
		result.append(", recoverableMode: ");
		if (recoverableModeESet) result.append(recoverableMode); else result.append("<unset>");
		result.append(", remoteSync: ");
		if (remoteSyncESet) result.append(remoteSync); else result.append("<unset>");
		result.append(", serverUrl: ");
		result.append(serverUrl);
		result.append(", synchronous: ");
		if (synchronousESet) result.append(synchronous); else result.append("<unset>");
		result.append(", synchronousEventTimeout: ");
		result.append(synchronousEventTimeout);
		result.append(", transactionTimeout: ");
		result.append(transactionTimeout);
		result.append(", workingDirectory: ");
		if (workingDirectoryESet) result.append(workingDirectory); else result.append("<unset>");
		result.append(')');
		return result.toString();
	}

} //MuleEnvironmentPropertiesTypeImpl
