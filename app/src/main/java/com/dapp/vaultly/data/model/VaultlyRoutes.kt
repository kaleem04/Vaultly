package com.dapp.vaultly.data.model

enum class VaultlyRoutes(
    val route: String
) {

    WELCOMESCREEN(route = "welcomeScreen"),
    VAULTLYBOTTOMSHEET(route = "bottomSheet"),
    PROFILESCREEN(route = "profileScreen"),
    DASHBOARDSCREEN(route = "dashboardScreen"),
    SPLASHSCREEN(route = "splashScreen");

    companion object {
        const val ADD_PASSWORD_WITH_ARG = "addPasswordScreen?id={id}"
    }

}