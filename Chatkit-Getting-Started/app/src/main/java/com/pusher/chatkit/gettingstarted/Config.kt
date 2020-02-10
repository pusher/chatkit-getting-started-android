package com.pusher.chatkit.gettingstarted

object Config {
    // TODO: supply your instance locator from the dashboard
    const val instanceLocator = "YOUR_INSTANCE_LOCATOR"

    const val userIdA = "pusher-quick-start-alice"
    const val userIdB = "pusher-quick-start-bob"

    val tokenProviderUrl = instanceLocator.split(":").let { (_, cluster, instanceId) ->
        "https://$cluster.pusherplatform.io/services/chatkit_token_provider/v1/$instanceId/token"
    }
}
