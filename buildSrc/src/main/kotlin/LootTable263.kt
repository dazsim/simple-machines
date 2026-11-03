import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

/** Rewrites a loot table into the 26.3 shape: functions to modifier, conditions to condition, "function"/"condition" keys to "type". */
class LootTable263(input: Reader) : FilterReader(StringReader(convert(input.readText()))) {
    companion object {
        fun convert(text: String): String = JsonOutput.prettyPrint(JsonOutput.toJson(walk(JsonSlurper().parseText(text))))

        private fun one(list: List<Any?>, wrap: (List<Any?>) -> Any?): Any? = if (list.size == 1) list[0] else wrap(list)

        private fun cond(c: Any?): Any? {
            if (c !is Map<*, *>) return c
            val m = LinkedHashMap<String, Any?>()
            c["condition"]?.let { m["type"] = it }
            for ((k, v) in c) if (k != "condition") m[k as String] = walk(v)
            if (m["type"] == "minecraft:block_state_property") {
                val r = LinkedHashMap<String, Any?>()
                r["type"] = "minecraft:match_block"; r["blocks"] = m["block"]; m["properties"]?.let { r["state"] = it }
                return r
            }
            return m
        }

        private fun fn(f: Any?): Any? {
            if (f !is Map<*, *>) return f
            val m = LinkedHashMap<String, Any?>()
            f["function"]?.let { m["type"] = it }
            for ((k, v) in f) if (k != "function") m[k as String] = walk(v)
            return m
        }

        fun walk(node: Any?): Any? = when (node) {
            is List<*> -> node.map { walk(it) }
            is Map<*, *> -> {
                val out = LinkedHashMap<String, Any?>()
                for ((key, v) in node) {
                    val k = key as String
                    when (k) {
                        "functions" -> out["modifier"] = one((v as List<*>).map { fn(it) }) { it }
                        "conditions" -> out["condition"] = one((v as List<*>).map { cond(it) }) { mapOf("type" to "minecraft:all_of", "terms" to it) }
                        "bonus_rolls" -> if ((v as Number).toDouble() != 0.0) out[k] = v
                        else -> (k as String).let { out[it] = if (it.contains(":")) v else walk(v) }
                    }
                }
                out
            }
            else -> node
        }
    }
}
