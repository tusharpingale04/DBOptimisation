package com.tushar.dboptimisation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private lateinit var btnOld: Button
    private lateinit var btnNew: Button
    private lateinit var btnDeleteUsers: Button
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOld = findViewById(R.id.btnOld)
        btnNew = findViewById(R.id.btnNew)
        btnDeleteUsers = findViewById(R.id.btnDeleteUsers)
        tvResult = findViewById(R.id.txtResult)

        btnOld.setOnClickListener {
            tvResult.text = ""
            runOldFlow()
        }
        btnNew.setOnClickListener {
            tvResult.text = ""
            runOptimisedFlow()
        }
        btnDeleteUsers.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@MainActivity)
                db.userDao().deleteUsers()
            }
        }
    }

    private fun runOptimisedFlow() {
        lifecycleScope.launch(Dispatchers.IO) {
            val jsonArray = loadJsonFromAsset(this@MainActivity, "users.json") ?: return@launch
            val users = mutableListOf<User>()
            for (i in 0 until jsonArray.length()) {
                val channelJson = jsonArray.optJSONObject(i)
                val email = channelJson.optString("email")
                val firstName = channelJson.optString("first_name")
                val lastName = channelJson.optString("last_name")
                val ipAddress = channelJson.optString("ip_address")
                val gender = channelJson.optString("gender")
                users.add(createUser(email, firstName, gender, ipAddress, lastName))
            }
            val db = AppDatabase.getInstance(this@MainActivity)
            var isLastItem = false
            var emittedItems = 0
            val time = measureTimeMillis {
                users.chunked(45).asFlow().onEach {
                    emittedItems += it.size
                    if (emittedItems >= users.size) {
                        isLastItem = true
                    }
                }.fold(UserAcc.Initial as UserAcc) { acc, value ->
                    val result = when (acc) {
                        UserAcc.Initial -> {
                            db.userDao().insertUsers(value)
                            UserAcc.NextUsers(emptyList())
                        }
                        is UserAcc.NextUsers -> {
                            if (acc.users.size >= 150 || isLastItem) {
                                val chats = acc.users + value
                                db.userDao().insertUsers(chats)
                                UserAcc.NextUsers(emptyList())
                            } else {
                                UserAcc.NextUsers(acc.users + value)
                            }
                        }
                    }
                    result
                }
            }
            withContext(Dispatchers.Main) {
                tvResult.text = "time taken: $time ms"
            }
        }
    }

    private fun runOldFlow() {
        lifecycleScope.launch(Dispatchers.IO) {
            val jsonArray = loadJsonFromAsset(this@MainActivity, "users.json") ?: return@launch
            val users = mutableListOf<User>()
            for (i in 0 until jsonArray.length()) {
                val channelJson = jsonArray.optJSONObject(i)
                val email = channelJson.optString("email")
                val firstName = channelJson.optString("first_name")
                val lastName = channelJson.optString("last_name")
                val ipAddress = channelJson.optString("ip_address")
                val gender = channelJson.optString("gender")
                users.add(createUser(email, firstName, gender, ipAddress, lastName))
            }
            val db = AppDatabase.getInstance(this@MainActivity)
            val time = measureTimeMillis {
                users.chunked(45).asFlow().collect {
                    db.userDao().insertUsers(it)
                }
            }
            withContext(Dispatchers.Main) {
                tvResult.text = "time taken: $time ms"
            }
        }
    }

    private fun loadJsonFromAsset(context: Context, fileName: String): JSONArray? {
        var jsonArray: JSONArray? = null
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            jsonArray = JSONArray(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return jsonArray
    }

    private fun createUser(
        email: String?,
        firstName: String?,
        gender: String?,
        ipAddress: String?,
        lastName: String?
    ): User {
        return User(
            email = email,
            firstName = firstName,
            gender = gender,
            ipAddress = ipAddress,
            lastName = lastName
        )
    }

    sealed class UserAcc {
        data object Initial : UserAcc()
        class NextUsers(val users: List<User>) : UserAcc()
    }
}