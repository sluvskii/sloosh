package com.slooshfilm.app.data.api

/** Canonical studio identifiers used by the catalog API and the iOS client. */
data class StudioBrand(
    val id: String,
    val name: String,
    val isNetwork: Boolean,
    val aliases: List<String>
) {
    companion object {
        val all = listOf(
            StudioBrand("marvel", "Marvel", false, listOf("marvel", "марвел")),
            StudioBrand("dc", "DC", false, listOf("dc", "диси")),
            StudioBrand("a24", "A24", false, listOf("a24")),
            StudioBrand("pixar", "Pixar", false, listOf("pixar", "пиксар")),
            StudioBrand("disney", "Disney", false, listOf("disney", "дисне")),
            StudioBrand("warner-bros", "Warner Bros.", false, listOf("warner", "уорнер")),
            StudioBrand("universal", "Universal", false, listOf("universal", "юниверсал")),
            StudioBrand("paramount", "Paramount", false, listOf("paramount", "парамаунт")),
            StudioBrand("20th-century-studios", "20th Century", false, listOf("20th century", "двадцатый век")),
            StudioBrand("sony-pictures", "Sony Pictures", false, listOf("sony", "сони")),
            StudioBrand("dreamworks", "DreamWorks", false, listOf("dreamworks", "дримворкс")),
            StudioBrand("netflix", "Netflix", true, listOf("netflix", "нетфликс")),
            StudioBrand("hbo", "HBO", true, listOf("hbo", "эйчби")),
            StudioBrand("apple-tv-plus", "Apple TV+", true, listOf("apple tv", "эппл тв")),
            StudioBrand("prime-video", "Prime Video", true, listOf("prime video", "прайм видео", "amazon")),
            StudioBrand("cartoon-network", "Cartoon Network", true, listOf("cartoon network", "картун нетворк")),
            StudioBrand("adult-swim", "Adult Swim", true, listOf("adult swim", "эдалт свим"))
        )

        fun find(value: String?): StudioBrand? {
            val clean = value?.trim()?.lowercase().orEmpty()
            if (clean.isEmpty()) return null
            return all.firstOrNull {
                it.id.lowercase() == clean || it.name.lowercase() == clean
            } ?: all.firstOrNull { brand -> brand.aliases.any { clean.contains(it.lowercase()) } }
        }
    }
}
