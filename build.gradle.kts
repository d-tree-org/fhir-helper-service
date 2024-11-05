tasks.register("downloadDependencies") {
    doLast {
        allprojects {
            configurations.all {
                try {
                    resolve()
                } catch (e: Exception) {
                    // Ignore resolution errors
                }
            }
        }
    }
}