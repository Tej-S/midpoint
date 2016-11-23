/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
public class MetadataPanel extends BasePanel<MetadataType>{

    private List<QName> metadataFieldsList = Arrays.asList(MetadataType.F_REQUEST_TIMESTAMP, MetadataType.F_REQUESTOR_REF,
            MetadataType.F_CREATE_TIMESTAMP, MetadataType.F_CREATOR_REF,
            MetadataType.F_CREATE_APPROVAL_TIMESTAMP, MetadataType.F_CREATE_APPROVER_REF,
            MetadataType.F_MODIFY_TIMESTAMP, MetadataType.F_MODIFIER_REF,
            MetadataType.F_MODIFY_APPROVAL_TIMESTAMP, MetadataType.F_MODIFY_APPROVER_REF);

    private static final String ID_METADATA_BLOCK = "metadataBlock";
    private static final String ID_METADATA_ROW = "metadataRow";
    private static final String ID_METADATA_PROPERTY_KEY = "metadataPropertyKey";
    private static final String ID_METADATA_FILED = "metadataField";
    private static final String DOT_CLASS = MetadataPanel.class.getSimpleName() + ".";

    public MetadataPanel(String id, IModel<MetadataType> model){
        super(id, model);
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer metadataBlock = new WebMarkupContainer(ID_METADATA_BLOCK);
        metadataBlock.setOutputMarkupId(true);
        add(metadataBlock);

        RepeatingView metadataRowRepeater = new RepeatingView(ID_METADATA_ROW);
        metadataBlock.add(metadataRowRepeater);
        for (QName qname : metadataFieldsList){
            WebMarkupContainer metadataRow = new WebMarkupContainer(metadataRowRepeater.newChildId());
            metadataRow.setOutputMarkupId(true);
            metadataRowRepeater.add(metadataRow);

            metadataRow.add(new Label(ID_METADATA_PROPERTY_KEY, createStringResource(DOT_CLASS + qname.getLocalPart())));
            metadataRow.add(new Label(ID_METADATA_FILED,
                    new AbstractReadOnlyModel<String>() {
                        @Override
                        public String getObject() {
                            PropertyModel<Object> tempModel = new PropertyModel<Object>(getModel(),
                                    qname.getLocalPart());
                            if (tempModel.getObject() instanceof XMLGregorianCalendar){
                                return WebComponentUtil.getLocalizedDate((XMLGregorianCalendar)tempModel.getObject(),
                                        DateLabelComponent.MEDIUM_SHORT_STYLE);
                            } else if (tempModel.getObject() instanceof ObjectReferenceType){
                                ObjectReferenceType ref = (ObjectReferenceType) tempModel.getObject();
                                return WebComponentUtil.getName(ref);
                            } else if (tempModel.getObject() instanceof List){
                                List list = (List) tempModel.getObject();
                                String result = "";
                                for (Object o : list){
                                    if (o instanceof  ObjectReferenceType){
                                        if (result.length() > 0){
                                            result += ", ";
                                        }
                                        result += WebComponentUtil.getName((ObjectReferenceType) o);
                                    }
                                }
                                return result;
                            }
                            return "";
                        }
                    }));

        }


    }
}
