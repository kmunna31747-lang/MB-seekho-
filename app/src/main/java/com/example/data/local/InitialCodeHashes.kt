package com.example.data.local

/**
 * Private SHA-256 hash inventory for the 50 initial Student codes (STU001..STU050)
 * and 10 initial Teacher codes (TCH001..TCH010).
 *
 * SECURITY: Raw access codes are NEVER stored in Android frontend source code,
 * resources, logs, or UI. Verification computes SHA-256 of user input and checks
 * against these hashes atomically in the database.
 */
object InitialCodeHashes {

    val studentCodeHashes: List<Pair<String, String>> = listOf(
        "STU001" to "df7fbb079937f591e790ea2f34ee1a43702765981bf209eec3d3b9beb85219d3",
        "STU002" to "5d27a2e39af127efe12d114f877c550eef7efbfadde77d5fb796e7c9746d01f7",
        "STU003" to "e6c17b8eaeb6d9f3a2461cb433455eb8854a54dd07377de124c10c490b44db76",
        "STU004" to "f92d657f1523d5cad10f7a15b91112efb1d534602411b056940921b3d99ab88d",
        "STU005" to "916a7f2c93ebcb455bccf4f25cfcec1b590fd71da7feed3169009a7842246dfd",
        "STU006" to "b153f3b90ca327efb2a85abb0d096e2810355182b87379b57c4831694a533343",
        "STU007" to "1eb3229565af3ad48faea5ae5affb2691e3077e99002cbffa98a3aa9fa084df5",
        "STU008" to "4da82f9c4ce2b84a45c79841963f71c3c6c31d45e54e4a2633f080e0faf3d995",
        "STU009" to "a2c95fe5efe818e99a10ba493bc9aa5ae0cb7d2a947a0b52b20dda9e4de8cbea",
        "STU010" to "e23a9e70db031ff5685049010d4bec1ff091ed0f0f3445b6c9aae3ee322bea25",
        "STU011" to "f3da0ee4cf1fe70135de192e6cf69c023248e42d46c27ed367531513622a6694",
        "STU012" to "65e7af718b72f3e50cd7cf6c7adacc3a53b2b6d83b07b91a000da9e8a27e49bc",
        "STU013" to "1c3beed486f18ba1d46ff271a9070d6caecd00bd94bebfd9e0eb65d47d176b3f",
        "STU014" to "328b9cf38c64ab993d2437834400f1b7be1c53027cbb180164848b7c313b7402",
        "STU015" to "da0fd6b73b24727bf6990888ceff7252a73490f0d720e7a7876300ea5db783ba",
        "STU016" to "00183682723ea1135ed2c9d7dd0f13cfd8bcf924cd52b8bcc12809e5e0ce9f46",
        "STU017" to "275e2c3c11d7a51163b65c322f90bd5fb9b57a683ae10d49b7b9d9481c89b998",
        "STU018" to "9b6181a1a87a35de2098b794c391b3bc10d1d61764ad4bad11209ad24e548b69",
        "STU019" to "d681305a63213088a7c1d95ee9522e0f1b0b10e5e47b56bbe1cb16977f112d6a",
        "STU020" to "eb21788e8579123b01b59d041d562a93a917b26e55e5deb9065fc0f58c597ff2",
        "STU021" to "9e260a33cb18cc6851226627134908bd7b89f71ae33d774ee3fc5137d58d9fb3",
        "STU022" to "4e80069270826b20a4a28ed93abfd5960b2da801ea69c7fe2cbc9ab1331587a3",
        "STU023" to "18e326578e407853bfd6f10b0d2d89c3d41a2035ec700168b15ca6e8ced52913",
        "STU024" to "c99f1dd05da5bdf50bb9dfc99879e4dd635744e5e3b99eda88188943da27e291",
        "STU025" to "a13c7421665e4a13f890e5e5a7dd456d821161de99852b67191f21828ac2c136",
        "STU026" to "e407043913feda8935d7eace490df094716a136e1ef94eb5b3be4f45c7223254",
        "STU027" to "ff36b09758ffdb100345fba46cb1af9a4531cd187de8544c30c1e19d6ea51fb8",
        "STU028" to "fb45844fb0da77efa1ac25d26a4dd73828fc32a9a7567c56433b4ebf66af8a0b",
        "STU029" to "bee9fa605aea5bcd78b0675e9fe7d5beafab4ba03c8bef19e0f6308f74df6a27",
        "STU030" to "0e5817bc2441cf1ff261a8956b9f746d79b942f01ec0be952b6da2fa093a8411",
        "STU031" to "08f77f4ea2665a00f28e3f51ee3a27eb7dc6e7a41df1bad06fa49a168c3e94f2",
        "STU032" to "572137f7cade3a697e95b9ba8be1a4752991fdfc63ec118412c4d9b062613d4b",
        "STU033" to "22e23ad6457239e3c74f07fa012739b09812178ec89fbf26dbc0fcdb00fd93a0",
        "STU034" to "a47d9b572841736d991ee176a39ec6e2a02711e2199b9ad5f033916caecf96ef",
        "STU035" to "3e598840cad8f9c6c7d8685bf2d92477293a54d6775ffe16487ed35eae822288",
        "STU036" to "e34c8119d45fdd0af8d192dfa889abd911c9b1bd77cdcefa5dffe4ccbb69367f",
        "STU037" to "f5cbfe01a481add41919a68f19892c0eaf5faa903d8f6aac9aa001f67a8f2a3b",
        "STU038" to "7a48e26daea8a0bd1e038330ac58facb7c1c997df0f939b2b0dbcfbc7148b5a9",
        "STU039" to "e06d640118c1f5c6521c8fb6b1eb6da25beaae0d9e5006fe97cf8361af79d54c",
        "STU040" to "202f872ab4ffcef6899d8e1fec7cdd99742d303cb91a7d3d15af175114238b4e",
        "STU041" to "358fbf249d549e03705e8252a5989f7e1835b605318f33d5e56e692a3d44f327",
        "STU042" to "7a076dc4b151c5870a83f83ffbf7d79c068e438f38e99d351aa2d87db13806c9",
        "STU043" to "7664453f6bc989b274c709efd604482526a0ef6238725f460a9ec953b11f0ca6",
        "STU044" to "a1b00676f5f987a189b6413d4f8681a6f99f205a17a637731667cf4d458b0afc",
        "STU045" to "ad0753fc6999314c8b1619d4d96bc89b360e073f0f10e9a6c40b60c0553d74ec",
        "STU046" to "89fb2ffd5a475dbbc8cbb77ca5685e3dea3580b22c34e6fa4c07a975c3102fb3",
        "STU047" to "836e55dbc819a2ca46d540ee0cdeb3e1c23ced8e276fd4302644ee0c348f82b3",
        "STU048" to "bdec2d1d5bed730836bd870ac925e85d554f888748d8b73db55a873b25b79b9f",
        "STU049" to "26058222f137f49ed74344e5ed46ee5ca285435974d8e772a7a4f30348e52613",
        "STU050" to "83b3c6513f47573f07e75e3009ab145e68346f5be68cf75b3aa40c847af721b4"
    )

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
