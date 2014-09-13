/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.io.Serializable;

/**
 * Describes a state of the operation. Although theoretically everything relevant should be in the model context,
 * as a convenience for clients interpreting this structure we offer explicit denotation of specific events (see EventType).
 *
 * HIGHLY EXPERIMENTAL. Probably should be refactored (e.g. by providing strong typing via class hierarchy).
 *
 * @author mederly
 */
public class OperationStatus implements Serializable {

    public enum EventType {
        NOTIFICATIONS,
        WORKFLOWS,
        RESOURCE_OBJECT_OPERATION,
        FOCUS_OPERATION
    }

    public enum StateType {
        ENTERING, EXITING;
    }

    private EventType eventType;
    private StateType stateType;
    private ResourceShadowDiscriminator resourceShadowDiscriminator;              // if relevant
    private OperationResult operationResult;                                      // if relevant (mostly on "_EXITING" events)

    public OperationStatus() {
    }

    public OperationStatus(EventType eventType) {
        this.eventType = eventType;
        this.stateType = StateType.ENTERING;
    }

    public OperationStatus(EventType eventType, OperationResult operationResult) {
        this.eventType = eventType;
        this.stateType = StateType.EXITING;
        this.operationResult = operationResult;
    }

    public OperationStatus(EventType eventType, ResourceShadowDiscriminator resourceShadowDiscriminator) {
        this(eventType);
        this.resourceShadowDiscriminator = resourceShadowDiscriminator;
    }

    public OperationStatus(EventType eventType, ResourceShadowDiscriminator resourceShadowDiscriminator, OperationResult operationResult) {
        this(eventType, operationResult);
        this.resourceShadowDiscriminator = resourceShadowDiscriminator;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public StateType getStateType() {
        return stateType;
    }

    public OperationResult getOperationResult() {
        return operationResult;
    }

    public void setOperationResult(OperationResult operationResult) {
        this.operationResult = operationResult;
    }

    public void setStateType(StateType stateType) {
        this.stateType = stateType;
    }

    public ResourceShadowDiscriminator getResourceShadowDiscriminator() {
        return resourceShadowDiscriminator;
    }

    public void setResourceShadowDiscriminator(ResourceShadowDiscriminator resourceShadowDiscriminator) {
        this.resourceShadowDiscriminator = resourceShadowDiscriminator;
    }
}