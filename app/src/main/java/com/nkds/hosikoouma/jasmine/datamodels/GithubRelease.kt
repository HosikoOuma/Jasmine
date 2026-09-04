package com.nkds.hosikoouma.jasmine.datamodels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String,
    @SerialName("body") val body: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<GithubAsset>
)

@Serializable
data class GithubAsset(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("browser_download_url") val downloadUrl: String
)
