@file:OptIn(ExperimentalSerializationApi::class)

package com.korilin.pintask.protocol.net

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber


@Serializable
class NetResponse(
    @ProtoNumber(0)
    val code: Int = 0,
    @ProtoNumber(1)
    val message: String = "",
    @ProtoNumber(2)
    val data: ByteArray? = null
)