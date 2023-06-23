import java.io.File

class UnSupportException(message: String) : RuntimeException(message)

val SuperClass = """
package com.korilin.pintask.protocol

abstract class ProtoGenEntity
"""

sealed class ProtoSyntax
enum class FieldType(val protoType: String, val kotlinType: String, val kotlinDefault: String) {
    Int32("int32", "Int", "0"),
    Str("string", "String?", "null"),
    ByteArray("bytes", "ByteArray?", "null");

    companion object {
        private val array = arrayOf(Int32, Str, ByteArray)

        fun fromProtoType(type: String): FieldType {
            return array.find { it.protoType == type } ?: throw NullPointerException("UnSupport ProtoType $type")
        }
    }
}

class SyntaxVersion(val version: String) : ProtoSyntax() {
    companion object {
        fun fromLine(line: String): SyntaxVersion {
            val start = line.indexOf("=")
            val end = line.indexOf(";")
            val version = line.substring(start + 1, end).trim()
            return SyntaxVersion(version)
        }
    }
}

class TargetPackageDir(val dir: File, val pck: TargetPackage)
class TargetPackage(val path: String) : ProtoSyntax() {
    fun makePackage(output: File): TargetPackageDir {
        val dir = File(output, path)
        dir.mkdirs()
        return TargetPackageDir(dir, this)
    }

    companion object {

        private const val TAG = "package"

        fun fromLine(line: String): TargetPackage {
            val start = line.indexOf(TAG) + TAG.length
            val end = line.indexOf(";")
            val path = line.substring(start + 1, end).trim()
            return TargetPackage(path)
        }
    }
}

class FieldDefined(
    val type: FieldType, val name: String, val number: String
) {

    val lowerCCName get(): String {
        val builder = StringBuilder()
        var upper = false
        for (ch in name) {
            val isTransverse = ch == '_'
            if (upper) {
                if (isTransverse) throw UnSupportException("Un support field, beacuse the name has two _ chat.")
                builder.append(ch.uppercaseChar())
                upper = false
            } else if (isTransverse) {
                upper = true
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun transformToKotlinField(): String {
        return """
            
            @ProtoNumber($number)
            var $lowerCCName: ${type.kotlinType} = ${type.kotlinDefault}
        """.replaceIndent("\t")
    }

    companion object {
        fun fromLine(line: String): FieldDefined {
            val regex = Regex("""\x20+""")
            val elements = line.trim().split(regex)
            val type = FieldType.fromProtoType(elements[0])
            val name = elements[1]
            val number = elements[3].removeSuffix(";")
            return FieldDefined(type, name, number)
        }
    }
}

class MessageStructure(val name: String) : ProtoSyntax() {

    var fields: List<FieldDefined> = emptyList()

    private fun genFileHeader(pck: TargetPackage): String {
        return """
        @file:OptIn(ExperimentalSerializationApi::class)

        package ${pck.path}

        import com.korilin.pintask.protocol.ProtoGenEntity
        import kotlinx.serialization.ExperimentalSerializationApi
        import kotlinx.serialization.Serializable
        import kotlinx.serialization.decodeFromByteArray
        import kotlinx.serialization.decodeFromHexString
        import kotlinx.serialization.protobuf.ProtoBuf
        import kotlinx.serialization.protobuf.ProtoNumber
        
        """.trimIndent()
    }

    private fun startClass(): String {
        return """
            
            // Generated Class from proto file
            @Serializable
            class $name: ProtoGenEntity() {
            
        """.trimIndent()
    }

    private fun classContent(): String {
        val builder = StringBuilder()
        fields.forEach {
            builder.append(it.transformToKotlinField())
            builder.append("\n")
        }
        return builder.toString()
    }

    private fun endClass(): String {
        return """
    companion object {
        fun fromByteArray(bytes: ByteArray): $name {
            return ProtoBuf.decodeFromByteArray(bytes)
        }

        fun fromHexString(hex: String): $name {
            return ProtoBuf.decodeFromHexString(hex)
        }
    }
}
        """
    }

    fun generateClassFile(tpd: TargetPackageDir) {
        val header = genFileHeader(tpd.pck)
        val start = startClass()
        val content = classContent()
        val end = endClass()
        val file = File(tpd.dir, "$name.kt")
        file.createNewFile()
        val writer = file.writer()
        writer.append(header)
        writer.append(start)
        writer.append(content)
        writer.append(end)
        writer.close()
        println("Create $file")
    }

    companion object {
        private const val TAG = "message"

        fun fromLine(line: String): MessageStructure {
            val start = line.indexOf(TAG) + TAG.length
            val end = line.indexOf("{")
            val name = line.substring(start + 1, end).trim()
            return MessageStructure(name)
        }
    }
}

fun parseProtoSyntax(file: File): List<ProtoSyntax> {
    val reader = file.reader()
    val lines = reader.readLines()
    val result = mutableListOf<ProtoSyntax>()
    var index = 0
    val all = lines.size
    while (index < all) {
        val line = lines[index]
        if (line.startsWith("syntax")) {
            val version = SyntaxVersion.fromLine(line)
            result.add(version)
        }
        if (line.startsWith("package")) {
            val pck = TargetPackage.fromLine(line)
            result.add(pck)
        }
        if (line.startsWith("message")) {
            val message = MessageStructure.fromLine(line)
            val fields = mutableListOf<FieldDefined>()
            while (true) {
                index++
                val contentLine = lines[index]
                if (contentLine.startsWith("}")) break
                val field = FieldDefined.fromLine(contentLine)
                fields.add(field)
                if (contentLine.endsWith("}")) break
            }
            message.fields = fields
            result.add(message)
        }
        index++
    }
    return result
}

println("Start Generate Protocol Kotln Classes")

val sourceRoot = File("proto")
val outputRoot = File("generate")

del(outputRoot)
outputRoot.mkdirs()

fun del(dir: File) {
    if (dir.isFile) dir.delete()
    if (dir.isDirectory) {
        for (file in dir.listFiles()) {
            del(file)
        }
        dir.delete()
    }
}

fun asProtoFiles(fileOrDir: File): List<File> {
    val files = mutableListOf<File>()
    if (fileOrDir.isFile && fileOrDir.name.endsWith(".proto")) {
        files.add(fileOrDir)
    }
    if (fileOrDir.isDirectory) {
        fileOrDir.listFiles()!!.forEach {
            files.addAll(asProtoFiles(it))
        }
    }
    return files
}

fun genSuperClass() {
    val dir = File(outputRoot, "com.korilin.pintask.protocol")
    dir.mkdirs()
    val file = File(dir, "ProtoGenEntity.kt")
    file.createNewFile()
    val writer = file.writer()
    writer.append(SuperClass)
    writer.close()
}


val files = asProtoFiles(sourceRoot)

genSuperClass()

files.forEach {
    println("parse $it")
    val result = parseProtoSyntax(it)
    var tpd: TargetPackageDir? = null
    result.forEach { syntax ->
        when (syntax) {
            is SyntaxVersion -> println("proto version: ${syntax.version}")
            is TargetPackage -> tpd = syntax.makePackage(outputRoot)
            is MessageStructure -> syntax.generateClassFile(tpd!!)
            else -> throw UnSupportException("UnSupport Syntax Type $syntax")
        }
    }
}