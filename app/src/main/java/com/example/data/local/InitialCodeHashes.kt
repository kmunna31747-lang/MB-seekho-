package com.example.data.local

/**
 * Private SHA-256 hash inventory for the 10 initial Teacher Access Codes (TCH001..TCH010).
 *
 * SECURITY:
 * - Zero Student Access Codes are hardcoded in the Android app or exposed in frontend code.
 *   Student Access Codes (10,000+ and unlimited future batches) are imported or generated
 *   securely through the Owner Admin system / Firebase backend and stored strictly as SHA-256 hashes.
 * - Raw Teacher Access Codes are never stored in source code, resources, logs, or UI.
 */
object InitialCodeHashes {

    val teacherCodeHashes: List<Pair<String, String>> = listOf(
        "TCH001" to "62f5b32b48d275ecdc0bb97626e7da0c2776bb9de96b690c27e774049eb2a76b",
        "TCH002" to "38b6c6344550302b3163ae8066611573efdfda47357083120b78e1b6c05ca3ba",
        "TCH003" to "0359e5b6212cdd39dfe6e7db7e2b9c79f6c2d6b936e47945f20918dd563220b3",
        "TCH004" to "e00bafd05b678928e90b1337947ac86a15c1e0a905ff392d8edb96f04ef863ad",
        "TCH005" to "ef6edbccc0da93b02d56907954e8bafda44eb6ec17ef40b2f82a698b7a852951",
        "TCH006" to "d4fb3f4d470441f439ad27e4ba588b44900844ad4e5ba3440b74e29035453c60",
        "TCH007" to "8bc97b42a97f7da9532b2ba4ef4b2c6077f841fb8f5c8f99c74f29506e290ce2",
        "TCH008" to "3be2da9416ec0489368cdffe020b445ff0f1589874f6d1de52ec23b910369203",
        "TCH009" to "8cd110400d14c46830e8c974a44fa86a66a483bceb53af7e6255742297b464ad",
        "TCH010" to "b17041f6dfbeb52078c4d0ce7cf0e85dabc587f2f3c8b4696879520abe348b95"
    )
}
