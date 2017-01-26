/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.policy.lifecycle.global;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.wf.impl.policy.lifecycle.AbstractTestLifecycle;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import javax.xml.namespace.QName;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.ObjectTypes.USER;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

/**
 * Tests role lifecycle with global policy rules.
 *
 * @author mederly
 */
public class TestLifecycleGlobal extends AbstractTestLifecycle {

	@Override
	protected boolean approveObjectAdd() {
		return true;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		/*
		    <!-- ruleAll -->
			<globalPolicyRule>
				<policyConstraints>
					<modification/>
				</policyConstraints>
				<policyActions>
					<approval>
					    <approvalSchema>
					    	<level>
								<approverRelation>owner</approverRelation>
								<outcomeIfNoApprovers>approve</outcomeIfNoApprovers>
							</level>
					    </approvalSchema>
					</approval>
				</policyActions>
				<focusSelector>
					<type>RoleType</type>
				</focusSelector>
			</globalPolicyRule>

			<!-- ruleAdd -->
			<globalPolicyRule>
				<policyConstraints>
					<modification>
						<operation>add</operation>
					</modification>
				</policyConstraints>
				<policyActions>
					<approval>
						<approverRef oid="...userLead1Oid..." type="UserType" />
					</approval>
				</policyActions>
				<focusSelector>
					<type>RoleType</type>
				</focusSelector>
			</globalPolicyRule>

		 */

		ObjectSelectorType focusSelector = new ObjectSelectorType(prismContext);
		focusSelector.setType(RoleType.COMPLEX_TYPE);

		GlobalPolicyRuleType ruleAll = new GlobalPolicyRuleType(prismContext);
		PolicyConstraintsType constraintsAll = new PolicyConstraintsType(prismContext);
		constraintsAll.getModification().add(new ModificationPolicyConstraintType(prismContext));
		ruleAll.setPolicyConstraints(constraintsAll);
		PolicyActionsType actionsAll = new PolicyActionsType(prismContext);
		ApprovalPolicyActionType approvalActionAll = new ApprovalPolicyActionType(prismContext);
		ApprovalSchemaType approvalSchema = new ApprovalSchemaType(prismContext);
		ApprovalLevelType level = new ApprovalLevelType(prismContext);
		level.getApproverRelation().add(new QName("owner"));		// intentionally no namespace
		level.setOutcomeIfNoApprovers(ApprovalLevelOutcomeType.APPROVE);
		approvalSchema.getLevel().add(level);
		approvalActionAll.setApprovalSchema(approvalSchema);
		actionsAll.setApproval(approvalActionAll);
		ruleAll.setFocusSelector(focusSelector.clone());
		ruleAll.setPolicyActions(actionsAll);

		GlobalPolicyRuleType ruleAdd = new GlobalPolicyRuleType(prismContext);
		PolicyConstraintsType constraintsAdd = new PolicyConstraintsType(prismContext);
		ModificationPolicyConstraintType modificationConstraintAdd = new ModificationPolicyConstraintType(prismContext);
		modificationConstraintAdd.getOperation().add(ChangeTypeType.ADD);
		constraintsAdd.getModification().add(modificationConstraintAdd);
		ruleAdd.setPolicyConstraints(constraintsAdd);
		PolicyActionsType actionsAdd = new PolicyActionsType(prismContext);
		ApprovalPolicyActionType approvalActionAdd = new ApprovalPolicyActionType(prismContext);
		approvalActionAdd.getApproverRef().add(createObjectRef(userLead1Oid, USER));
		actionsAdd.setApproval(approvalActionAdd);
		ruleAdd.setFocusSelector(focusSelector.clone());
		ruleAdd.setPolicyActions(actionsAdd);

		List<ItemDelta<?, ?>> deltas =
				DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
						.item(SystemConfigurationType.F_GLOBAL_POLICY_RULE)
						.replace(ruleAll, ruleAdd)
						.asItemDeltas();
		repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), deltas, initResult);

		systemObjectCache.invalidateCaches();

		IntegrationTestTools.display("System configuration",
				getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value()));
	}
}
