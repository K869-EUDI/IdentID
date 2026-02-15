/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package com.k869.identid.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.k869.identid.navigation.routes.common.featureCommonGraph
import com.k869.identid.navigation.routes.dashboard.featureDashboardGraph
import com.k869.identid.navigation.routes.issuance.featureIssuanceGraph
import com.k869.identid.navigation.routes.presentation.presentationGraph
import com.k869.identid.navigation.routes.proximity.featureProximityGraph
import com.k869.identid.navigation.routes.startup.featureStartupGraph
import com.k869.identid.ui.container.EudiComponentActivity

class MainActivity : EudiComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Content(intent) {
                featureStartupGraph(it)
                featureCommonGraph(it)
                featureDashboardGraph(it)
                presentationGraph(it)
                featureProximityGraph(it)
                featureIssuanceGraph(it)
            }
        }
    }
}