/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.util;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import org.mule.schema.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage
 * @generated
 */
public class MuleSwitch {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static MulePackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleSwitch() {
		if (modelPackage == null) {
			modelPackage = MulePackage.eINSTANCE;
		}
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	public Object doSwitch(EObject theEObject) {
		return doSwitch(theEObject.eClass(), theEObject);
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	protected Object doSwitch(EClass theEClass, EObject theEObject) {
		if (theEClass.eContainer() == modelPackage) {
			return doSwitch(theEClass.getClassifierID(), theEObject);
		}
		else {
			List eSuperTypes = theEClass.getESuperTypes();
			return
				eSuperTypes.isEmpty() ?
					defaultCase(theEObject) :
					doSwitch((EClass)eSuperTypes.get(0), theEObject);
		}
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	protected Object doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case MulePackage.AGENTS_TYPE: {
				AgentsType agentsType = (AgentsType)theEObject;
				Object result = caseAgentsType(agentsType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.AGENT_TYPE: {
				AgentType agentType = (AgentType)theEObject;
				Object result = caseAgentType(agentType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.CATCH_ALL_STRATEGY_TYPE: {
				CatchAllStrategyType catchAllStrategyType = (CatchAllStrategyType)theEObject;
				Object result = caseCatchAllStrategyType(catchAllStrategyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.COMPONENT_FACTORY_TYPE: {
				ComponentFactoryType componentFactoryType = (ComponentFactoryType)theEObject;
				Object result = caseComponentFactoryType(componentFactoryType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE: {
				ComponentLifecycleAdapterFactoryType componentLifecycleAdapterFactoryType = (ComponentLifecycleAdapterFactoryType)theEObject;
				Object result = caseComponentLifecycleAdapterFactoryType(componentLifecycleAdapterFactoryType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.COMPONENT_POOL_FACTORY_TYPE: {
				ComponentPoolFactoryType componentPoolFactoryType = (ComponentPoolFactoryType)theEObject;
				Object result = caseComponentPoolFactoryType(componentPoolFactoryType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.CONNECTION_STRATEGY_TYPE: {
				ConnectionStrategyType connectionStrategyType = (ConnectionStrategyType)theEObject;
				Object result = caseConnectionStrategyType(connectionStrategyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.CONNECTOR_TYPE: {
				ConnectorType connectorType = (ConnectorType)theEObject;
				Object result = caseConnectorType(connectorType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.CONSTRAINT_TYPE: {
				ConstraintType constraintType = (ConstraintType)theEObject;
				Object result = caseConstraintType(constraintType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.CONTAINER_CONTEXT_TYPE: {
				ContainerContextType containerContextType = (ContainerContextType)theEObject;
				Object result = caseContainerContextType(containerContextType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.CONTAINER_ENTRY_TYPE: {
				ContainerEntryType containerEntryType = (ContainerEntryType)theEObject;
				Object result = caseContainerEntryType(containerEntryType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.CONTAINER_PROPERTY_TYPE: {
				ContainerPropertyType containerPropertyType = (ContainerPropertyType)theEObject;
				Object result = caseContainerPropertyType(containerPropertyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.DOCUMENT_ROOT: {
				DocumentRoot documentRoot = (DocumentRoot)theEObject;
				Object result = caseDocumentRoot(documentRoot);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ENCRYPTION_STRATEGY_TYPE: {
				EncryptionStrategyType encryptionStrategyType = (EncryptionStrategyType)theEObject;
				Object result = caseEncryptionStrategyType(encryptionStrategyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ENDPOINT_IDENTIFIERS_TYPE: {
				EndpointIdentifiersType endpointIdentifiersType = (EndpointIdentifiersType)theEObject;
				Object result = caseEndpointIdentifiersType(endpointIdentifiersType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ENDPOINT_IDENTIFIER_TYPE: {
				EndpointIdentifierType endpointIdentifierType = (EndpointIdentifierType)theEObject;
				Object result = caseEndpointIdentifierType(endpointIdentifierType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ENDPOINT_TYPE: {
				EndpointType endpointType = (EndpointType)theEObject;
				Object result = caseEndpointType(endpointType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ENTRY_POINT_RESOLVER_TYPE: {
				EntryPointResolverType entryPointResolverType = (EntryPointResolverType)theEObject;
				Object result = caseEntryPointResolverType(entryPointResolverType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ENTRY_TYPE: {
				EntryType entryType = (EntryType)theEObject;
				Object result = caseEntryType(entryType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE: {
				EnvironmentPropertiesType environmentPropertiesType = (EnvironmentPropertiesType)theEObject;
				Object result = caseEnvironmentPropertiesType(environmentPropertiesType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.EXCEPTION_STRATEGY_TYPE: {
				ExceptionStrategyType exceptionStrategyType = (ExceptionStrategyType)theEObject;
				Object result = caseExceptionStrategyType(exceptionStrategyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.FACTORY_ENTRY_TYPE: {
				FactoryEntryType factoryEntryType = (FactoryEntryType)theEObject;
				Object result = caseFactoryEntryType(factoryEntryType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.FACTORY_PROPERTY_TYPE: {
				FactoryPropertyType factoryPropertyType = (FactoryPropertyType)theEObject;
				Object result = caseFactoryPropertyType(factoryPropertyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.FILE_PROPERTIES_TYPE: {
				FilePropertiesType filePropertiesType = (FilePropertiesType)theEObject;
				Object result = caseFilePropertiesType(filePropertiesType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.FILTER_TYPE: {
				FilterType filterType = (FilterType)theEObject;
				Object result = caseFilterType(filterType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.GLOBAL_ENDPOINTS_TYPE: {
				GlobalEndpointsType globalEndpointsType = (GlobalEndpointsType)theEObject;
				Object result = caseGlobalEndpointsType(globalEndpointsType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.GLOBAL_ENDPOINT_TYPE: {
				GlobalEndpointType globalEndpointType = (GlobalEndpointType)theEObject;
				Object result = caseGlobalEndpointType(globalEndpointType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.INBOUND_ROUTER_TYPE: {
				InboundRouterType inboundRouterType = (InboundRouterType)theEObject;
				Object result = caseInboundRouterType(inboundRouterType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.INTERCEPTOR_STACK_TYPE: {
				InterceptorStackType interceptorStackType = (InterceptorStackType)theEObject;
				Object result = caseInterceptorStackType(interceptorStackType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.INTERCEPTOR_TYPE: {
				InterceptorType interceptorType = (InterceptorType)theEObject;
				Object result = caseInterceptorType(interceptorType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.LEFT_FILTER_TYPE: {
				LeftFilterType leftFilterType = (LeftFilterType)theEObject;
				Object result = caseLeftFilterType(leftFilterType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.LIST_TYPE: {
				ListType listType = (ListType)theEObject;
				Object result = caseListType(listType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.MAP_TYPE: {
				MapType mapType = (MapType)theEObject;
				Object result = caseMapType(mapType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.MODEL_TYPE: {
				ModelType modelType = (ModelType)theEObject;
				Object result = caseModelType(modelType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.MULE_CONFIGURATION_TYPE: {
				MuleConfigurationType muleConfigurationType = (MuleConfigurationType)theEObject;
				Object result = caseMuleConfigurationType(muleConfigurationType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.MULE_DESCRIPTOR_TYPE: {
				MuleDescriptorType muleDescriptorType = (MuleDescriptorType)theEObject;
				Object result = caseMuleDescriptorType(muleDescriptorType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE: {
				MuleEnvironmentPropertiesType muleEnvironmentPropertiesType = (MuleEnvironmentPropertiesType)theEObject;
				Object result = caseMuleEnvironmentPropertiesType(muleEnvironmentPropertiesType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.OUTBOUND_ROUTER_TYPE: {
				OutboundRouterType outboundRouterType = (OutboundRouterType)theEObject;
				Object result = caseOutboundRouterType(outboundRouterType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.PERSISTENCE_STRATEGY_TYPE: {
				PersistenceStrategyType persistenceStrategyType = (PersistenceStrategyType)theEObject;
				Object result = casePersistenceStrategyType(persistenceStrategyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.POOLING_PROFILE_TYPE: {
				PoolingProfileType poolingProfileType = (PoolingProfileType)theEObject;
				Object result = casePoolingProfileType(poolingProfileType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.PROPERTIES_TYPE: {
				PropertiesType propertiesType = (PropertiesType)theEObject;
				Object result = casePropertiesType(propertiesType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.PROPERTY_TYPE: {
				PropertyType propertyType = (PropertyType)theEObject;
				Object result = casePropertyType(propertyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.QUEUE_PROFILE_TYPE: {
				QueueProfileType queueProfileType = (QueueProfileType)theEObject;
				Object result = caseQueueProfileType(queueProfileType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.REPLY_TO_TYPE: {
				ReplyToType replyToType = (ReplyToType)theEObject;
				Object result = caseReplyToType(replyToType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.RESPONSE_ROUTER_TYPE: {
				ResponseRouterType responseRouterType = (ResponseRouterType)theEObject;
				Object result = caseResponseRouterType(responseRouterType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.RIGHT_FILTER_TYPE: {
				RightFilterType rightFilterType = (RightFilterType)theEObject;
				Object result = caseRightFilterType(rightFilterType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.ROUTER_TYPE: {
				RouterType routerType = (RouterType)theEObject;
				Object result = caseRouterType(routerType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.SECURITY_FILTER_TYPE: {
				SecurityFilterType securityFilterType = (SecurityFilterType)theEObject;
				Object result = caseSecurityFilterType(securityFilterType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.SECURITY_MANAGER_TYPE: {
				SecurityManagerType securityManagerType = (SecurityManagerType)theEObject;
				Object result = caseSecurityManagerType(securityManagerType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.SECURITY_PROVIDER_TYPE: {
				SecurityProviderType securityProviderType = (SecurityProviderType)theEObject;
				Object result = caseSecurityProviderType(securityProviderType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.SYSTEM_ENTRY_TYPE: {
				SystemEntryType systemEntryType = (SystemEntryType)theEObject;
				Object result = caseSystemEntryType(systemEntryType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.SYSTEM_PROPERTY_TYPE: {
				SystemPropertyType systemPropertyType = (SystemPropertyType)theEObject;
				Object result = caseSystemPropertyType(systemPropertyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.TEXT_PROPERTY_TYPE: {
				TextPropertyType textPropertyType = (TextPropertyType)theEObject;
				Object result = caseTextPropertyType(textPropertyType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.THREADING_PROFILE_TYPE: {
				ThreadingProfileType threadingProfileType = (ThreadingProfileType)theEObject;
				Object result = caseThreadingProfileType(threadingProfileType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.TRANSACTION_MANAGER_TYPE: {
				TransactionManagerType transactionManagerType = (TransactionManagerType)theEObject;
				Object result = caseTransactionManagerType(transactionManagerType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.TRANSACTION_TYPE: {
				TransactionType transactionType = (TransactionType)theEObject;
				Object result = caseTransactionType(transactionType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.TRANSFORMERS_TYPE: {
				TransformersType transformersType = (TransformersType)theEObject;
				Object result = caseTransformersType(transformersType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case MulePackage.TRANSFORMER_TYPE: {
				TransformerType transformerType = (TransformerType)theEObject;
				Object result = caseTransformerType(transformerType);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Agents Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Agents Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseAgentsType(AgentsType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Agent Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Agent Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseAgentType(AgentType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Catch All Strategy Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Catch All Strategy Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseCatchAllStrategyType(CatchAllStrategyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Component Factory Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Component Factory Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseComponentFactoryType(ComponentFactoryType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Component Lifecycle Adapter Factory Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Component Lifecycle Adapter Factory Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseComponentLifecycleAdapterFactoryType(ComponentLifecycleAdapterFactoryType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Component Pool Factory Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Component Pool Factory Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseComponentPoolFactoryType(ComponentPoolFactoryType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Connection Strategy Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Connection Strategy Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseConnectionStrategyType(ConnectionStrategyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Connector Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Connector Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseConnectorType(ConnectorType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Constraint Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Constraint Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseConstraintType(ConstraintType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Container Context Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Container Context Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseContainerContextType(ContainerContextType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Container Entry Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Container Entry Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseContainerEntryType(ContainerEntryType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Container Property Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Container Property Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseContainerPropertyType(ContainerPropertyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Document Root</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Document Root</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseDocumentRoot(DocumentRoot object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Encryption Strategy Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Encryption Strategy Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseEncryptionStrategyType(EncryptionStrategyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Endpoint Identifiers Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Endpoint Identifiers Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseEndpointIdentifiersType(EndpointIdentifiersType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Endpoint Identifier Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Endpoint Identifier Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseEndpointIdentifierType(EndpointIdentifierType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Endpoint Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Endpoint Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseEndpointType(EndpointType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Entry Point Resolver Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Entry Point Resolver Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseEntryPointResolverType(EntryPointResolverType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Entry Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Entry Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseEntryType(EntryType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Environment Properties Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Environment Properties Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseEnvironmentPropertiesType(EnvironmentPropertiesType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Exception Strategy Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Exception Strategy Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseExceptionStrategyType(ExceptionStrategyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Factory Entry Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Factory Entry Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseFactoryEntryType(FactoryEntryType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Factory Property Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Factory Property Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseFactoryPropertyType(FactoryPropertyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>File Properties Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>File Properties Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseFilePropertiesType(FilePropertiesType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Filter Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Filter Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseFilterType(FilterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Global Endpoints Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Global Endpoints Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseGlobalEndpointsType(GlobalEndpointsType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Global Endpoint Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Global Endpoint Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseGlobalEndpointType(GlobalEndpointType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Inbound Router Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Inbound Router Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseInboundRouterType(InboundRouterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Interceptor Stack Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Interceptor Stack Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseInterceptorStackType(InterceptorStackType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Interceptor Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Interceptor Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseInterceptorType(InterceptorType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Left Filter Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Left Filter Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseLeftFilterType(LeftFilterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>List Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>List Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseListType(ListType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Map Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Map Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseMapType(MapType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Model Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Model Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseModelType(ModelType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Configuration Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Configuration Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseMuleConfigurationType(MuleConfigurationType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Descriptor Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Descriptor Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseMuleDescriptorType(MuleDescriptorType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Environment Properties Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Environment Properties Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseMuleEnvironmentPropertiesType(MuleEnvironmentPropertiesType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Outbound Router Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Outbound Router Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseOutboundRouterType(OutboundRouterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Persistence Strategy Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Persistence Strategy Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object casePersistenceStrategyType(PersistenceStrategyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Pooling Profile Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Pooling Profile Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object casePoolingProfileType(PoolingProfileType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Properties Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Properties Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object casePropertiesType(PropertiesType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Property Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Property Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object casePropertyType(PropertyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Queue Profile Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Queue Profile Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseQueueProfileType(QueueProfileType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Reply To Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Reply To Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseReplyToType(ReplyToType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Response Router Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Response Router Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseResponseRouterType(ResponseRouterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Right Filter Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Right Filter Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseRightFilterType(RightFilterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Router Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Router Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseRouterType(RouterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Security Filter Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Security Filter Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseSecurityFilterType(SecurityFilterType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Security Manager Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Security Manager Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseSecurityManagerType(SecurityManagerType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Security Provider Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Security Provider Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseSecurityProviderType(SecurityProviderType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>System Entry Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>System Entry Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseSystemEntryType(SystemEntryType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>System Property Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>System Property Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseSystemPropertyType(SystemPropertyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Text Property Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Text Property Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseTextPropertyType(TextPropertyType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Threading Profile Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Threading Profile Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseThreadingProfileType(ThreadingProfileType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Transaction Manager Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Transaction Manager Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseTransactionManagerType(TransactionManagerType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Transaction Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Transaction Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseTransactionType(TransactionType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Transformers Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Transformers Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseTransformersType(TransformersType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Transformer Type</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Transformer Type</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseTransformerType(TransformerType object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	public Object defaultCase(EObject object) {
		return null;
	}

} //MuleSwitch
