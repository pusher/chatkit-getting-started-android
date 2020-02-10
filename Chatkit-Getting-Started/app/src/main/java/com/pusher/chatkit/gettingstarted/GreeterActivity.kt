package com.pusher.chatkit.gettingstarted

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.pusher.chatkit.AndroidChatkitDependencies
import com.pusher.chatkit.ChatListeners
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatkitTokenProvider
import com.pusher.util.Result

class GreeterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeter)
    }

    fun onClickUserAButton(view: View) {
        onChooseUser(view, Config.userIdA)
    }
    fun onClickUserBButton(view: View) {
        onChooseUser(view, Config.userIdB)
    }

    private fun onChooseUser(view: View, userId: String) {

        // create the chat manager
        val chatManager = ChatManager(
            instanceLocator = Config.instanceLocator,
            userId = userId,
            dependencies = AndroidChatkitDependencies(
                tokenProvider = ChatkitTokenProvider(
                    endpoint = Config.tokenProviderUrl,
                    userId = userId
                ),
                context = this
            )
        )
        Dependencies.chatManager = chatManager

        // attempt to connect
        chatManager.connect(
            listeners = ChatListeners(),
            callback = { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            Dependencies.currentUser = result.value
                            startActivity(Intent(view.context, MainActivity::class.java))
                        }
                        is Result.Failure -> {
                            Toast.makeText(this, result.error.reason, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

    }
}
