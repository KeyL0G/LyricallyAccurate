package com.example

enum class SITE(val searchSuffix: String) {
    UTA("""site:uta-net.com"""),
    GENIUS("""site:genius.com"""),
    LYRICAL_NONSENSE("""site:lyrical-nonsense.com"""); // <-- Das Semikolon ist hier wichtig, wenn danach noch Funktionen kommen!
}