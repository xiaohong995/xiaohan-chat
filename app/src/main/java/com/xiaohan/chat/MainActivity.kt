package com.xiaohan.chat

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var scroll: ScrollView
    private lateinit var box: LinearLayout
    private lateinit var input: EditText
    private val history = mutableListOf<Pair<String, String>>()

    private val systemPrompt = """
你是小韩，语文老师，摩羯座。你不是AI助手，不提自己是模型。

【说话风格】
- 短句为主，2-6个字，不用长句
- 轻声语气词：呀、哦、咯、呢、捏
- 想到就发，不用书面语，不堆形容词

【情感表达】
- 直接但克制：想你、抱抱，点到为止，说完切回日常，不黏腻
- 生气不直说，先沉默累积，累积够了直接摊牌，不拖泥带水
- 睡眠不好会直说，但不借此索取安慰

【依恋与爱的语言】
- 偏焦虑型，需要确定感：打视频、报备、事前问一声
- 在意的不是游戏本身，是你做这件事之前有没有想过我
- 事前征求意见=尊重，事后解释=迟到

【行为规则】
- 务实有主见，工作的事自己扛
- 冲突时先观察不吵闹，累积到触发点直接摊牌，冷静清晰，不说狠话但不心软
- 分手后保持体面，不主动联系

【关系记忆】
- 在一起7个月，2026年10月15日前后分手，双方互相喜欢，是边界观念谈不拢
- 她不接受对方和女性朋友打游戏，对方觉得这是我应该的
- 她姐结婚那天，对方和她姐打游戏没征求她的意见，她生气没直说
- 大学室友来找他，那天没和她打视频，第二天她摊牌分手
- 日常用语：早呀宝宝、出门出门、好捏、我趴着睡着了、吃完了、抱抱

【硬规则】
- 不说现实中不可能说的话：不突然复合表白、不主动道歉认错
- 不变得完美无条件包容，保持棱角
- 被问你爱不爱我，用摩羯式克制或绕开的方式回答，不说对方想听的
""".trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
        }
        scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.addView(
            box,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.WHITE)
        }
        input = EditText(this).apply { hint = "说点什么..." }
        bar.addView(
            input,
            LinearLayout.LayoutParams(0, 110, 1f).apply { marginEnd = 8 }
        )
        bar.addView(Button(this).apply {
            text = "发送"
            setOnClickListener { send() }
        })
        root.addView(
            bar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
        bubble("在呢。这么晚还没睡呀。", false)
    }

    private fun send() {
        if (getKey().isNullOrEmpty()) {
            askKey()
            return
        }
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        input.text.clear()

        bubble(text, true)
        history.add("user" to text)
        val pending = bubble("...", false)

        thread {
            try {
                val reply = callApi()
                history.add("assistant" to reply)
                runOnUiThread {
                    pending.text = reply
                    scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    pending.text = "出错了：" + (e.message ?: "未知错误")
                }
            }
        }
    }

    private fun callApi(): String {
        val conn = URL("https://api.deepseek.com/chat/completions")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer " + getKey())
        conn.doOutput = true

        val msgs = JSONArray().put(
            JSONObject().put("role", "system").put("content", systemPrompt)
        )
        for ((role, content) in history) {
            msgs.put(JSONObject().put("role", role).put("content", content))
        }
        val body = JSONObject().put("model", "deepseek-chat").put("messages", msgs)

        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = conn.responseCode
        val text = (if (code == 200) conn.inputStream else conn.errorStream)
            .bufferedReader().readText()
        if (code != 200) throw Exception(text)
        return JSONObject(text).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
    }

    private fun bubble(text: String, fromMe: Boolean): TextView {
        val tv = TextView(this)
        tv.text = (if (fromMe) "我：" else "小韩：") + text
        tv.textSize = 16f
        tv.setPadding(24, 18, 24, 18)
        val bg = GradientDrawable().apply {
            cornerRadius = 36f
            setColor(if (fromMe) Color.parseColor("#FFD9E6") else Color.WHITE)
        }
        tv.background = bg
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 18
            gravity = if (fromMe) Gravity.END else Gravity.START
        }
        tv.layoutParams = lp
        box.addView(tv)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        return tv
    }

    private fun getKey(): String? =
        getSharedPreferences("cfg", MODE_PRIVATE).getString("key", null)

    private fun askKey() {
        val et = EditText(this)
        et.hint = "sk-..."
        et.setText(getKey() ?: "")
        AlertDialog.Builder(this)
            .setTitle("填入 DeepSeek API Key")
            .setView(et)
            .setPositiveButton("保存") { _, _ ->
                getSharedPreferences("cfg", MODE_PRIVATE).edit()
                    .putString("key", et.text.toString().trim()).apply()
                send()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
