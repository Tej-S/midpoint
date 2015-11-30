/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author mederly
 */
@Component
public class AccCertQueryHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertQueryHelper.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelService modelService;

    @Autowired
    private MatchingRuleRegistry matchingRuleRegistry;

    @Autowired
    protected AccCertGeneralHelper helper;

    // TODO temporary hack because of some problems in model service...
    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    protected List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectQuery newQuery;
        InOidFilter inOidFilter = InOidFilter.createOwnerHasOidIn(campaignOid);
        if (query == null) {
            newQuery = ObjectQuery.createObjectQuery(inOidFilter);
        } else {
            newQuery = query.clone();
            if (query.getFilter() == null) {
                newQuery.setFilter(inOidFilter);
            } else {
                newQuery.setFilter(AndFilter.createAnd(query.getFilter(), inOidFilter));
            }
        }

        newQuery = hackPaging(newQuery);

        List<AccessCertificationCaseType> caseList = repositoryService.searchContainers(AccessCertificationCaseType.class, newQuery, options, result);
        return caseList;
    }

    /**
     * Maps from "old style" of specifying sorting criteria to current one:
     *   targetRef -> targetRef/@/name
     *   objectRef -> objectRef/@/name
     *   campaignRef -> ../name
     *
     * Temporary solution - until we implement that in GUI.
     */
    private ObjectQuery hackPaging(ObjectQuery query) {
        if (query.getPaging() == null || ItemPath.isNullOrEmpty(query.getPaging().getOrderBy())) {
            return query;
        }
        ItemPath oldPath = query.getPaging().getOrderBy();
        if (oldPath.size() != 1 || !(oldPath.first() instanceof NameItemPathSegment)) {
            return query;
        }
        QName oldName = ((NameItemPathSegment) oldPath.first()).getName();
        ItemPath newPath;
        if (QNameUtil.match(oldName, AccessCertificationCaseType.F_TARGET_REF)) {
            newPath = new ItemPath(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_OBJECT_REF)) {
            newPath = new ItemPath(AccessCertificationCaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
        } else if (QNameUtil.match(oldName, AccessCertificationCaseType.F_CAMPAIGN_REF)) {
            newPath = new ItemPath(PrismConstants.T_PARENT, ObjectType.F_NAME);
        } else {
            return query;
        }
        ObjectPaging paging1 = query.getPaging().clone();
        paging1.setOrderBy(newPath);
        ObjectQuery query1 = query.clone();
        query1.setPaging(paging1);
        return query1;
    }

    protected List<AccessCertificationCaseType> searchDecisions(ObjectQuery query, String reviewerOid, boolean notDecidedOnly, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {

        // TODO FILTER BY NOT_DECIDED_ONLY

        // enhance filter with reviewerRef + enabled
        ObjectQuery newQuery;
        ObjectReferenceType reviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
        ObjectFilter reviewerFilter = RefFilter.createReferenceEqual(
                new ItemPath(AccessCertificationCaseType.F_REVIEWER_REF),
                AccessCertificationCaseType.class, prismContext, reviewerRef.asReferenceValue());
        ObjectFilter enabledFilter = EqualFilter.createEqual(
                AccessCertificationCaseType.F_ENABLED, AccessCertificationCaseType.class, prismContext, Boolean.TRUE);
        ObjectFilter reviewerAndEnabledFilter = AndFilter.createAnd(reviewerFilter, enabledFilter);
        if (query == null) {
            newQuery = ObjectQuery.createObjectQuery(reviewerAndEnabledFilter);
        } else {
            newQuery = query.clone();
            if (query.getFilter() == null) {
                newQuery.setFilter(reviewerAndEnabledFilter);
            } else {
                newQuery.setFilter(AndFilter.createAnd(query.getFilter(), reviewerAndEnabledFilter));
            }
        }

        newQuery = hackPaging(newQuery);

        // retrieve cases, filtered
        List<AccessCertificationCaseType> caseList = repositoryService.searchContainers(AccessCertificationCaseType.class, newQuery, options, result);

        // campaigns already loaded
        Map<String,AccessCertificationCampaignType> campaigns = new HashMap<>();

        // remove irrelevant decisions from each case and add campaignRef
        for (AccessCertificationCaseType _case : caseList) {
            if (_case.getCampaignRef() == null) {
                LOGGER.warn("AccessCertificationCaseType {} has no campaignRef -- skipping it", _case);
                continue;
            }
            // obtain campaign object
            String campaignOid = _case.getCampaignRef().getOid();
            AccessCertificationCampaignType campaign = campaigns.get(campaignOid);
            if (campaign == null) {
                campaign = repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result).asObjectable();    // TODO error checking + call model instead of repo
                campaigns.put(campaignOid, campaign);
            }

            int stage = campaign.getCurrentStageNumber();
            Iterator<AccessCertificationDecisionType> decisionIterator = _case.getDecision().iterator();
            while (decisionIterator.hasNext()) {
                AccessCertificationDecisionType decision = decisionIterator.next();
                if (decision.getStageNumber() != stage || !decision.getReviewerRef().getOid().equals(reviewerOid)) {
                    decisionIterator.remove();
                }
            }

            PrismObject<AccessCertificationCampaignType> campaignObject = campaign.asPrismObject();
            ObjectReferenceType campaignRef = ObjectTypeUtil.createObjectRef(campaignObject);
            _case.setCampaignRef(campaignRef);
            _case.getCampaignRef().asReferenceValue().setObject(campaignObject);    // has to be done AFTER setCampaignRef in order to preserve the value!
        }

        return caseList;
    }

    // we expect that only one decision item (the relevant one) is present
    private boolean isDecided(AccessCertificationCaseType _case) {
        if (_case.getDecision() == null || _case.getDecision().isEmpty()) {
            return false;
        }
        if (_case.getDecision().size() > 1) {
            throw new IllegalStateException("More than 1 decision in case");
        }
        AccessCertificationResponseType response = _case.getDecision().get(0).getResponse();
        return response != null && response != AccessCertificationResponseType.NO_RESPONSE;
    }

    public List<AccessCertificationCaseType> getCasesForReviewer(AccessCertificationCampaignType campaign,
                                                                 String reviewerOid, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
        ObjectFilter filter = AndFilter.createAnd(
                RefFilter.createReferenceEqual(
                        new ItemPath(AccessCertificationCaseType.F_REVIEWER_REF),
                        AccessCertificationCaseType.class,
                        prismContext,
                        ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER).asReferenceValue()),
                EqualFilter.createEqual(
                        new ItemPath(AccessCertificationCaseType.F_ENABLED),
                        AccessCertificationCaseType.class,
                        prismContext,
                        true)
                );

        List<AccessCertificationCaseType> caseList = searchCases(campaign.getOid(), ObjectQuery.createObjectQuery(filter), null, task, result);
        return caseList;
    }

    public AccessCertificationCaseType getCase(String campaignOid, long caseId, OperationResult result) throws SchemaException {
        ObjectFilter filter = AndFilter.createAnd(
                InOidFilter.createOwnerHasOidIn(campaignOid),
                InOidFilter.createInOid(String.valueOf(caseId))
        );
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<AccessCertificationCaseType> caseList = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
        if (caseList.isEmpty()) {
            return null;
        } else if (caseList.size() == 1) {
            return caseList.get(0);
        } else {
            throw new IllegalStateException("More than one certification case with ID " + caseId + " in campaign " + campaignOid);
        }
    }

    public List<AccessCertificationCaseType> selectCasesForReviewer(List<AccessCertificationCaseType> caseList, String reviewerOid) {
        List<AccessCertificationCaseType> rv = new ArrayList<>();
        for (AccessCertificationCaseType aCase : caseList) {
            if (Boolean.TRUE.equals(aCase.isEnabled())) {
                for (ObjectReferenceType reviewerRef : aCase.getReviewerRef()) {
                    if (reviewerOid.equals(reviewerRef.getOid())) {
                        rv.add(aCase.clone());
                        break;
                    }
                }
            }
        }
        return rv;
    }
}
