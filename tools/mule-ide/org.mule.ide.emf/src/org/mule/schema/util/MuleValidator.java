/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.util;

import java.util.Collection;
import java.util.Map;

import org.eclipse.emf.common.util.DiagnosticChain;

import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.EObjectValidator;

import org.eclipse.emf.ecore.xml.type.util.XMLTypeValidator;

import org.mule.schema.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Validator</b> for the model.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage
 * @generated
 */
public class MuleValidator extends EObjectValidator {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final MuleValidator INSTANCE = new MuleValidator();

	/**
	 * A constant for the {@link org.eclipse.emf.common.util.Diagnostic#getSource() source} of diagnostic {@link org.eclipse.emf.common.util.Diagnostic#getCode() codes} from this package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.common.util.Diagnostic#getSource()
	 * @see org.eclipse.emf.common.util.Diagnostic#getCode()
	 * @generated
	 */
	public static final String DIAGNOSTIC_SOURCE = "org.mule.schema";

	/**
	 * A constant with a fixed name that can be used as the base value for additional hand written constants.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final int GENERATED_DIAGNOSTIC_CODE_COUNT = 0;

	/**
	 * A constant with a fixed name that can be used as the base value for additional hand written constants in a derived class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static final int DIAGNOSTIC_CODE_COUNT = GENERATED_DIAGNOSTIC_CODE_COUNT;

	/**
	 * The cached base package validator.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected XMLTypeValidator xmlTypeValidator;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleValidator() {
		super();
		xmlTypeValidator = XMLTypeValidator.INSTANCE;
	}

	/**
	 * Returns the package of this validator switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EPackage getEPackage() {
	  return MulePackage.eINSTANCE;
	}

	/**
	 * Calls <code>validateXXX</code> for the corresonding classifier of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected boolean validate(int classifierID, Object value, DiagnosticChain diagnostics, Map context) {
		switch (classifierID) {
			case MulePackage.AGENTS_TYPE:
				return validateAgentsType((AgentsType)value, diagnostics, context);
			case MulePackage.AGENT_TYPE:
				return validateAgentType((AgentType)value, diagnostics, context);
			case MulePackage.CATCH_ALL_STRATEGY_TYPE:
				return validateCatchAllStrategyType((CatchAllStrategyType)value, diagnostics, context);
			case MulePackage.COMPONENT_FACTORY_TYPE:
				return validateComponentFactoryType((ComponentFactoryType)value, diagnostics, context);
			case MulePackage.COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE:
				return validateComponentLifecycleAdapterFactoryType((ComponentLifecycleAdapterFactoryType)value, diagnostics, context);
			case MulePackage.COMPONENT_POOL_FACTORY_TYPE:
				return validateComponentPoolFactoryType((ComponentPoolFactoryType)value, diagnostics, context);
			case MulePackage.CONNECTION_STRATEGY_TYPE:
				return validateConnectionStrategyType((ConnectionStrategyType)value, diagnostics, context);
			case MulePackage.CONNECTOR_TYPE:
				return validateConnectorType((ConnectorType)value, diagnostics, context);
			case MulePackage.CONSTRAINT_TYPE:
				return validateConstraintType((ConstraintType)value, diagnostics, context);
			case MulePackage.CONTAINER_CONTEXT_TYPE:
				return validateContainerContextType((ContainerContextType)value, diagnostics, context);
			case MulePackage.CONTAINER_ENTRY_TYPE:
				return validateContainerEntryType((ContainerEntryType)value, diagnostics, context);
			case MulePackage.CONTAINER_PROPERTY_TYPE:
				return validateContainerPropertyType((ContainerPropertyType)value, diagnostics, context);
			case MulePackage.DOCUMENT_ROOT:
				return validateDocumentRoot((DocumentRoot)value, diagnostics, context);
			case MulePackage.ENCRYPTION_STRATEGY_TYPE:
				return validateEncryptionStrategyType((EncryptionStrategyType)value, diagnostics, context);
			case MulePackage.ENDPOINT_IDENTIFIERS_TYPE:
				return validateEndpointIdentifiersType((EndpointIdentifiersType)value, diagnostics, context);
			case MulePackage.ENDPOINT_IDENTIFIER_TYPE:
				return validateEndpointIdentifierType((EndpointIdentifierType)value, diagnostics, context);
			case MulePackage.ENDPOINT_TYPE:
				return validateEndpointType((EndpointType)value, diagnostics, context);
			case MulePackage.ENTRY_POINT_RESOLVER_TYPE:
				return validateEntryPointResolverType((EntryPointResolverType)value, diagnostics, context);
			case MulePackage.ENTRY_TYPE:
				return validateEntryType((EntryType)value, diagnostics, context);
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE:
				return validateEnvironmentPropertiesType((EnvironmentPropertiesType)value, diagnostics, context);
			case MulePackage.EXCEPTION_STRATEGY_TYPE:
				return validateExceptionStrategyType((ExceptionStrategyType)value, diagnostics, context);
			case MulePackage.FACTORY_ENTRY_TYPE:
				return validateFactoryEntryType((FactoryEntryType)value, diagnostics, context);
			case MulePackage.FACTORY_PROPERTY_TYPE:
				return validateFactoryPropertyType((FactoryPropertyType)value, diagnostics, context);
			case MulePackage.FILE_PROPERTIES_TYPE:
				return validateFilePropertiesType((FilePropertiesType)value, diagnostics, context);
			case MulePackage.FILTER_TYPE:
				return validateFilterType((FilterType)value, diagnostics, context);
			case MulePackage.GLOBAL_ENDPOINTS_TYPE:
				return validateGlobalEndpointsType((GlobalEndpointsType)value, diagnostics, context);
			case MulePackage.GLOBAL_ENDPOINT_TYPE:
				return validateGlobalEndpointType((GlobalEndpointType)value, diagnostics, context);
			case MulePackage.INBOUND_ROUTER_TYPE:
				return validateInboundRouterType((InboundRouterType)value, diagnostics, context);
			case MulePackage.INTERCEPTOR_STACK_TYPE:
				return validateInterceptorStackType((InterceptorStackType)value, diagnostics, context);
			case MulePackage.INTERCEPTOR_TYPE:
				return validateInterceptorType((InterceptorType)value, diagnostics, context);
			case MulePackage.LEFT_FILTER_TYPE:
				return validateLeftFilterType((LeftFilterType)value, diagnostics, context);
			case MulePackage.LIST_TYPE:
				return validateListType((ListType)value, diagnostics, context);
			case MulePackage.MAP_TYPE:
				return validateMapType((MapType)value, diagnostics, context);
			case MulePackage.MODEL_TYPE:
				return validateModelType((ModelType)value, diagnostics, context);
			case MulePackage.MULE_CONFIGURATION_TYPE:
				return validateMuleConfigurationType((MuleConfigurationType)value, diagnostics, context);
			case MulePackage.MULE_DESCRIPTOR_TYPE:
				return validateMuleDescriptorType((MuleDescriptorType)value, diagnostics, context);
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE:
				return validateMuleEnvironmentPropertiesType((MuleEnvironmentPropertiesType)value, diagnostics, context);
			case MulePackage.OUTBOUND_ROUTER_TYPE:
				return validateOutboundRouterType((OutboundRouterType)value, diagnostics, context);
			case MulePackage.PERSISTENCE_STRATEGY_TYPE:
				return validatePersistenceStrategyType((PersistenceStrategyType)value, diagnostics, context);
			case MulePackage.POOLING_PROFILE_TYPE:
				return validatePoolingProfileType((PoolingProfileType)value, diagnostics, context);
			case MulePackage.PROPERTIES_TYPE:
				return validatePropertiesType((PropertiesType)value, diagnostics, context);
			case MulePackage.PROPERTY_TYPE:
				return validatePropertyType((PropertyType)value, diagnostics, context);
			case MulePackage.QUEUE_PROFILE_TYPE:
				return validateQueueProfileType((QueueProfileType)value, diagnostics, context);
			case MulePackage.REPLY_TO_TYPE:
				return validateReplyToType((ReplyToType)value, diagnostics, context);
			case MulePackage.RESPONSE_ROUTER_TYPE:
				return validateResponseRouterType((ResponseRouterType)value, diagnostics, context);
			case MulePackage.RIGHT_FILTER_TYPE:
				return validateRightFilterType((RightFilterType)value, diagnostics, context);
			case MulePackage.ROUTER_TYPE:
				return validateRouterType((RouterType)value, diagnostics, context);
			case MulePackage.SECURITY_FILTER_TYPE:
				return validateSecurityFilterType((SecurityFilterType)value, diagnostics, context);
			case MulePackage.SECURITY_MANAGER_TYPE:
				return validateSecurityManagerType((SecurityManagerType)value, diagnostics, context);
			case MulePackage.SECURITY_PROVIDER_TYPE:
				return validateSecurityProviderType((SecurityProviderType)value, diagnostics, context);
			case MulePackage.SYSTEM_ENTRY_TYPE:
				return validateSystemEntryType((SystemEntryType)value, diagnostics, context);
			case MulePackage.SYSTEM_PROPERTY_TYPE:
				return validateSystemPropertyType((SystemPropertyType)value, diagnostics, context);
			case MulePackage.TEXT_PROPERTY_TYPE:
				return validateTextPropertyType((TextPropertyType)value, diagnostics, context);
			case MulePackage.THREADING_PROFILE_TYPE:
				return validateThreadingProfileType((ThreadingProfileType)value, diagnostics, context);
			case MulePackage.TRANSACTION_MANAGER_TYPE:
				return validateTransactionManagerType((TransactionManagerType)value, diagnostics, context);
			case MulePackage.TRANSACTION_TYPE:
				return validateTransactionType((TransactionType)value, diagnostics, context);
			case MulePackage.TRANSFORMERS_TYPE:
				return validateTransformersType((TransformersType)value, diagnostics, context);
			case MulePackage.TRANSFORMER_TYPE:
				return validateTransformerType((TransformerType)value, diagnostics, context);
			case MulePackage.ACTION_TYPE:
				return validateActionType((Object)value, diagnostics, context);
			case MulePackage.CREATE_CONNECTOR_TYPE:
				return validateCreateConnectorType((Object)value, diagnostics, context);
			case MulePackage.ENABLE_CORRELATION_TYPE:
				return validateEnableCorrelationType((Object)value, diagnostics, context);
			case MulePackage.EXHAUSTED_ACTION_TYPE:
				return validateExhaustedActionType((Object)value, diagnostics, context);
			case MulePackage.ID_TYPE:
				return validateIdType((Object)value, diagnostics, context);
			case MulePackage.INITIALISATION_POLICY_TYPE:
				return validateInitialisationPolicyType((Object)value, diagnostics, context);
			case MulePackage.INITIAL_STATE_TYPE:
				return validateInitialStateType((Object)value, diagnostics, context);
			case MulePackage.POOL_EXHAUSTED_ACTION_TYPE:
				return validatePoolExhaustedActionType((Object)value, diagnostics, context);
			case MulePackage.TYPE_TYPE1:
				return validateTypeType1((Object)value, diagnostics, context);
			case MulePackage.ACTION_TYPE_OBJECT:
				return validateActionTypeObject((ActionType)value, diagnostics, context);
			case MulePackage.CREATE_CONNECTOR_TYPE_OBJECT:
				return validateCreateConnectorTypeObject((CreateConnectorType)value, diagnostics, context);
			case MulePackage.ENABLE_CORRELATION_TYPE_OBJECT:
				return validateEnableCorrelationTypeObject((EnableCorrelationType)value, diagnostics, context);
			case MulePackage.EXHAUSTED_ACTION_TYPE_OBJECT:
				return validateExhaustedActionTypeObject((ExhaustedActionType)value, diagnostics, context);
			case MulePackage.ID_TYPE_OBJECT:
				return validateIdTypeObject((IdType)value, diagnostics, context);
			case MulePackage.INITIALISATION_POLICY_TYPE_OBJECT:
				return validateInitialisationPolicyTypeObject((InitialisationPolicyType)value, diagnostics, context);
			case MulePackage.INITIAL_STATE_TYPE_OBJECT:
				return validateInitialStateTypeObject((InitialStateType)value, diagnostics, context);
			case MulePackage.POOL_EXHAUSTED_ACTION_TYPE_OBJECT:
				return validatePoolExhaustedActionTypeObject((PoolExhaustedActionType)value, diagnostics, context);
			case MulePackage.TYPE_TYPE:
				return validateTypeType((String)value, diagnostics, context);
			case MulePackage.TYPE_TYPE_OBJECT:
				return validateTypeTypeObject((TypeType1)value, diagnostics, context);
			case MulePackage.VERSION_TYPE:
				return validateVersionType((String)value, diagnostics, context);
			default: 
				return true;
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateAgentsType(AgentsType agentsType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(agentsType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateAgentType(AgentType agentType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(agentType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCatchAllStrategyType(CatchAllStrategyType catchAllStrategyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(catchAllStrategyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentFactoryType(ComponentFactoryType componentFactoryType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(componentFactoryType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentLifecycleAdapterFactoryType(ComponentLifecycleAdapterFactoryType componentLifecycleAdapterFactoryType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(componentLifecycleAdapterFactoryType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateComponentPoolFactoryType(ComponentPoolFactoryType componentPoolFactoryType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(componentPoolFactoryType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateConnectionStrategyType(ConnectionStrategyType connectionStrategyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(connectionStrategyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateConnectorType(ConnectorType connectorType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(connectorType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateConstraintType(ConstraintType constraintType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(constraintType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateContainerContextType(ContainerContextType containerContextType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(containerContextType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateContainerEntryType(ContainerEntryType containerEntryType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(containerEntryType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateContainerPropertyType(ContainerPropertyType containerPropertyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(containerPropertyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateDocumentRoot(DocumentRoot documentRoot, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(documentRoot, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEncryptionStrategyType(EncryptionStrategyType encryptionStrategyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(encryptionStrategyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEndpointIdentifiersType(EndpointIdentifiersType endpointIdentifiersType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(endpointIdentifiersType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEndpointIdentifierType(EndpointIdentifierType endpointIdentifierType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(endpointIdentifierType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEndpointType(EndpointType endpointType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(endpointType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEntryPointResolverType(EntryPointResolverType entryPointResolverType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(entryPointResolverType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEntryType(EntryType entryType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(entryType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEnvironmentPropertiesType(EnvironmentPropertiesType environmentPropertiesType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(environmentPropertiesType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateExceptionStrategyType(ExceptionStrategyType exceptionStrategyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(exceptionStrategyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFactoryEntryType(FactoryEntryType factoryEntryType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(factoryEntryType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFactoryPropertyType(FactoryPropertyType factoryPropertyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(factoryPropertyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFilePropertiesType(FilePropertiesType filePropertiesType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(filePropertiesType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateFilterType(FilterType filterType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(filterType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateGlobalEndpointsType(GlobalEndpointsType globalEndpointsType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(globalEndpointsType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateGlobalEndpointType(GlobalEndpointType globalEndpointType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(globalEndpointType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInboundRouterType(InboundRouterType inboundRouterType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(inboundRouterType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInterceptorStackType(InterceptorStackType interceptorStackType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(interceptorStackType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInterceptorType(InterceptorType interceptorType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(interceptorType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateLeftFilterType(LeftFilterType leftFilterType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(leftFilterType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateListType(ListType listType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(listType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMapType(MapType mapType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(mapType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateModelType(ModelType modelType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(modelType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMuleConfigurationType(MuleConfigurationType muleConfigurationType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(muleConfigurationType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMuleDescriptorType(MuleDescriptorType muleDescriptorType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(muleDescriptorType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateMuleEnvironmentPropertiesType(MuleEnvironmentPropertiesType muleEnvironmentPropertiesType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(muleEnvironmentPropertiesType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateOutboundRouterType(OutboundRouterType outboundRouterType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(outboundRouterType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePersistenceStrategyType(PersistenceStrategyType persistenceStrategyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(persistenceStrategyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePoolingProfileType(PoolingProfileType poolingProfileType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(poolingProfileType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePropertiesType(PropertiesType propertiesType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(propertiesType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePropertyType(PropertyType propertyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(propertyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateQueueProfileType(QueueProfileType queueProfileType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(queueProfileType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateReplyToType(ReplyToType replyToType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(replyToType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateResponseRouterType(ResponseRouterType responseRouterType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(responseRouterType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRightFilterType(RightFilterType rightFilterType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(rightFilterType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateRouterType(RouterType routerType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(routerType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateSecurityFilterType(SecurityFilterType securityFilterType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(securityFilterType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateSecurityManagerType(SecurityManagerType securityManagerType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(securityManagerType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateSecurityProviderType(SecurityProviderType securityProviderType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(securityProviderType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateSystemEntryType(SystemEntryType systemEntryType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(systemEntryType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateSystemPropertyType(SystemPropertyType systemPropertyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(systemPropertyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTextPropertyType(TextPropertyType textPropertyType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(textPropertyType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateThreadingProfileType(ThreadingProfileType threadingProfileType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(threadingProfileType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTransactionManagerType(TransactionManagerType transactionManagerType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(transactionManagerType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTransactionType(TransactionType transactionType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(transactionType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTransformersType(TransformersType transformersType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(transformersType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTransformerType(TransformerType transformerType, DiagnosticChain diagnostics, Map context) {
		return validate_EveryDefaultConstraint(transformerType, diagnostics, context);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateActionType(Object actionType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCreateConnectorType(Object createConnectorType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEnableCorrelationType(Object enableCorrelationType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateExhaustedActionType(Object exhaustedActionType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateIdType(Object idType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInitialisationPolicyType(Object initialisationPolicyType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInitialStateType(Object initialStateType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePoolExhaustedActionType(Object poolExhaustedActionType, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTypeType1(Object typeType1, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateActionTypeObject(ActionType actionTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateCreateConnectorTypeObject(CreateConnectorType createConnectorTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateEnableCorrelationTypeObject(EnableCorrelationType enableCorrelationTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateExhaustedActionTypeObject(ExhaustedActionType exhaustedActionTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateIdTypeObject(IdType idTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInitialisationPolicyTypeObject(InitialisationPolicyType initialisationPolicyTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateInitialStateTypeObject(InitialStateType initialStateTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validatePoolExhaustedActionTypeObject(PoolExhaustedActionType poolExhaustedActionTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTypeType(String typeType, DiagnosticChain diagnostics, Map context) {
		boolean result = validateTypeType_Enumeration(typeType, diagnostics, context);
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @see #validateTypeType_Enumeration
	 */
	public static final Collection TYPE_TYPE__ENUMERATION__VALUES =
		wrapEnumerationValues
			(new Object[] {
				 "seda",
				 "direct",
				 "pipeline",
				 "jms",
				 "jms-clustered",
				 "jcyclone",
				 "custom"
			 });

	/**
	 * Validates the Enumeration constraint of '<em>Type Type</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTypeType_Enumeration(String typeType, DiagnosticChain diagnostics, Map context) {
		boolean result = TYPE_TYPE__ENUMERATION__VALUES.contains(typeType);
		if (!result && diagnostics != null) 
			reportEnumerationViolation(MulePackage.eINSTANCE.getTypeType(), typeType, TYPE_TYPE__ENUMERATION__VALUES, diagnostics, context);
		return result; 
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateTypeTypeObject(TypeType1 typeTypeObject, DiagnosticChain diagnostics, Map context) {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateVersionType(String versionType, DiagnosticChain diagnostics, Map context) {
		boolean result = validateVersionType_Enumeration(versionType, diagnostics, context);
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @see #validateVersionType_Enumeration
	 */
	public static final Collection VERSION_TYPE__ENUMERATION__VALUES =
		wrapEnumerationValues
			(new Object[] {
				 "1.0"
			 });

	/**
	 * Validates the Enumeration constraint of '<em>Version Type</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean validateVersionType_Enumeration(String versionType, DiagnosticChain diagnostics, Map context) {
		boolean result = VERSION_TYPE__ENUMERATION__VALUES.contains(versionType);
		if (!result && diagnostics != null) 
			reportEnumerationViolation(MulePackage.eINSTANCE.getVersionType(), versionType, VERSION_TYPE__ENUMERATION__VALUES, diagnostics, context);
		return result; 
	}

} //MuleValidator
