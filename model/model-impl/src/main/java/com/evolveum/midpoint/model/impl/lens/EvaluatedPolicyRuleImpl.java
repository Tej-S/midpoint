/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRuleImpl implements EvaluatedPolicyRule {
	private static final long serialVersionUID = 1L;

	private final PolicyRuleType policyRuleType;
	private final AssignmentPath assignmentPath;
	private final Collection<EvaluatedPolicyRuleTrigger> triggers = new ArrayList<>();
	private final Collection<PolicyExceptionType> policyExceptions = new ArrayList<>();

	public EvaluatedPolicyRuleImpl(PolicyRuleType policyRuleType, AssignmentPath assignmentPath) {
		this.policyRuleType = policyRuleType;
		this.assignmentPath = assignmentPath;
	}

	@Override
	public String getName() {
		if (policyRuleType == null) {
			return null;
		}
		return policyRuleType.getName();
	}
	
	@Override
	public PolicyRuleType getPolicyRule() {
		return policyRuleType;
	}

	@Override
	public AssignmentPath getAssignmentPath() {
		return assignmentPath;
	}

	@Override
	public PolicyConstraintsType getPolicyConstraints() {
		return policyRuleType.getPolicyConstraints();
	}

	@NotNull
	@Override
	public Collection<EvaluatedPolicyRuleTrigger> getTriggers() {
		return triggers;
	}
	
	public void addTrigger(EvaluatedPolicyRuleTrigger trigger) {
		triggers.add(trigger);
	}

	@NotNull
	@Override
	public Collection<PolicyExceptionType> getPolicyExceptions() {
		return policyExceptions;
	}
	
	public void addPolicyException(PolicyExceptionType exception) {
		policyExceptions.add(exception);
	}


	@Override
	public PolicyActionsType getActions() {
		return policyRuleType.getPolicyActions();
	}
	
	@Override
	public String getPolicySituation() {
		// TODO default situations depending on getTriggeredConstraintKinds
		if (policyRuleType.getPolicySituation() != null) {
			return policyRuleType.getPolicySituation();
		}
		
		if (!triggers.isEmpty()) {
			EvaluatedPolicyRuleTrigger firstTrigger = triggers.iterator().next();
			if (firstTrigger instanceof EvaluatedSituationTrigger) {
				Collection<EvaluatedPolicyRule> sourceRules = ((EvaluatedSituationTrigger) firstTrigger).getSourceRules();
				if (!sourceRules.isEmpty()) {	// should be always the case
					return sourceRules.iterator().next().getPolicySituation();
				}
			}
			PolicyConstraintKindType constraintKind = firstTrigger.getConstraintKind();
			PredefinedPolicySituation predefSituation = PredefinedPolicySituation.get(constraintKind);
			if (predefSituation != null) {
				return predefSituation.getUrl();
			}
		}
		
		PolicyConstraintsType policyConstraints = getPolicyConstraints();
		if (policyConstraints.getExclusion() != null) {
			return PredefinedPolicySituation.EXCLUSION_VIOLATION.getUrl();
		}
		if (policyConstraints.getMinAssignees() != null) {
			return PredefinedPolicySituation.UNDERASSIGNED.getUrl();
		}
		if (policyConstraints.getMaxAssignees() != null) {
			return PredefinedPolicySituation.OVERASSIGNED.getUrl();
		}
		if (policyConstraints.getModification() != null) {
			return PredefinedPolicySituation.MODIFIED.getUrl();
		}
		if (policyConstraints.getAssignment() != null) {
			return PredefinedPolicySituation.ASSIGNED.getUrl();
		}
		return null;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluatedPolicyRule", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "policyRuleType", policyRuleType.toString(), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "assignmentPath", assignmentPath, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "triggers", triggers, indent + 1);
		return sb.toString();
	}

	@Override
	public EvaluatedPolicyRuleType toEvaluatedPolicyRuleType() {
		EvaluatedPolicyRuleType rv = new EvaluatedPolicyRuleType();
		rv.setPolicyRule(policyRuleType);
		triggers.forEach(t -> rv.getTrigger().add(t.toEvaluatedPolicyRuleTriggerType()));
		return rv;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((policyRuleType == null) ? 0 : policyRuleType.hashCode());
		result = prime * result + ((triggers == null) ? 0 : triggers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		EvaluatedPolicyRuleImpl other = (EvaluatedPolicyRuleImpl) obj;
		if (policyRuleType == null) {
			if (other.policyRuleType != null) {
				return false;
			}
		} else if (!policyRuleType.equals(other.policyRuleType)) {
			return false;
		}
		if (triggers == null) {
			if (other.triggers != null) {
				return false;
			}
		} else if (!triggers.equals(other.triggers)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "EvaluatedPolicyRuleImpl(" + getName() + ")";
	}

}
