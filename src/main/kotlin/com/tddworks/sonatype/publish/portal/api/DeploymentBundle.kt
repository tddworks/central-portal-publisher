package com.tddworks.sonatype.publish.portal.api

import java.io.File

data class DeploymentBundle(val file: File, val publicationType: PublicationType)
