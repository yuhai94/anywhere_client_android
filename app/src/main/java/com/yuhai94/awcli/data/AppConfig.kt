package com.yuhai94.awcli.data

data class AppConfig(
    val sshIp: String,
    val sshPort: Int,
    val sshUser: String,
    val sshPassword: String,
    val serviceIp: String,
    val servicePort: Int
) {
    fun isComplete(): Boolean {
        return sshIp.isNotBlank() &&
            sshPort > 0 &&
            sshUser.isNotBlank() &&
            sshPassword.isNotBlank() &&
            serviceIp.isNotBlank() &&
            servicePort > 0
    }
}
