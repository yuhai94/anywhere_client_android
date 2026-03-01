package com.yuhai94.awcli.data

import com.google.gson.annotations.SerializedName

data class InstanceSummary(
    val id: Long,
    val uuid: String,
    @SerializedName("ec2_id")
    val ec2Id: String?,
    @SerializedName("ec2_region")
    val ec2Region: String?,
    @SerializedName("ec2_region_name")
    val ec2RegionName: String?,
    @SerializedName("ec2_public_ip")
    val ec2PublicIp: String?,
    @SerializedName("direct_link")
    val directLink: String?,
    @SerializedName("relay_link")
    val relayLink: String?,
    val status: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class InstanceDetail(
    val id: Long,
    val uuid: String,
    @SerializedName("ec2_id")
    val ec2Id: String?,
    @SerializedName("ec2_region")
    val ec2Region: String?,
    @SerializedName("ec2_region_name")
    val ec2RegionName: String?,
    @SerializedName("ec2_public_ip")
    val ec2PublicIp: String?,
    @SerializedName("direct_link")
    val directLink: String?,
    @SerializedName("relay_link")
    val relayLink: String?,
    val status: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class CreateInstanceRequest(
    val region: String
)

data class CreateInstanceResponse(
    val uuid: String?,
    val status: String?,
    val error: String?
)

data class DeleteInstanceResponse(
    val status: String?,
    val error: String?
)

data class ApiError(
    val error: String?
)

data class RegionInfo(
    val region: String,
    val name: String
)
