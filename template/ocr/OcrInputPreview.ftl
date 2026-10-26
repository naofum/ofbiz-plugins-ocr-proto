<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#if imageUrl?has_content>
    <#if previewMode == "PDF">
        <iframe src="${imageUrl}" title="${uiLabelMap.OcrInputPreview?html}"
                style="width:100%; height:600px; border:0;"></iframe>
    <#elseif previewMode == "IMAGE">
        <img src="${imageUrl}" alt="${uiLabelMap.OcrInputPreview?html}"
             style="max-width:100%; height:auto; display:block; margin:0 auto;"/>
    <#else>
        <p>${uiLabelMap.OcrPreviewUnavailable?html}</p>
    </#if>
    <div style="margin-top:0.5rem;">
        <a href="${imageUrl}" target="_blank" rel="noopener">
            <#if previewMode == "OTHER">
                ${uiLabelMap.OcrDownloadOriginalFile?html}
            <#else>
                ${uiLabelMap.OcrOpenOriginalFile?html}
            </#if>
        </a>
    </div>
<#else>
    <span>${uiLabelMap.OcrNoInputImage?html}</span>
</#if>
