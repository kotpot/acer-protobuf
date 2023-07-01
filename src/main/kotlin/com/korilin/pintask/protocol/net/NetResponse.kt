@file:OptIn(ExperimentalSerializationApi::class)

package com.korilin.pintask.protocol.net

import com.korilin.pintask.protocol.net.NetResponse.Code.COMMON_ERROR
import com.korilin.pintask.protocol.net.NetResponse.Code.SUCCESS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber


@Serializable
class NetResponse(
    @ProtoNumber(0)
    val code: Int = 0,
    @ProtoNumber(1)
    val message: String = "",
    @ProtoNumber(2)
    val data: ByteArray? = null
) {

    fun success() = code == SUCCESS

    fun asHexString() = ProtoBuf.encodeToHexString(this)

    companion object Code {
        const val SUCCESS = 1
        const val COMMON_ERROR = 0
    }
}