package com.example.data

import com.example.model.ChecklistItem
import com.example.model.EmergencyType
import java.util.UUID

object EmergencyTemplates {

    val LOCATIONS = listOf(
        "Hathras शहर",
        "Naya Ganj, Hathras",
        "Talab Chauraha, Hathras",
        "Aligarh Road, Hathras",
        "Agra Road, Hathras",
        "Mursan Road, Hathras",
        "Iglas Road, Hathras",
        "Sadabad Gate, Hathras",
        "Mendu Gate, Hathras",
        "Chamar Gate, Hathras",
        "Hathras Junction",
        "Hathras City Station",
        "Hathras Bus Stand",
        "District Hospital, Hathras",
        "Collectorate, Hathras",
        "Old Police Line, Hathras",
        "Mursan",
        "Sasni",
        "Chandpa",
        "Sadabad",
        "Sikandra Rao",
        "Hasayan",
        "Sahpau",
        "Mendu",
        "Purdil Nagar",
        "🚔 थाना Hathras (Kotwali)",
        "🚔 थाना Hathras Gate",
        "🚔 थाना Hathras Junction",
        "🚔 थाना Mursan",
        "🚔 थाना Sasni",
        "🚔 थाना Sadabad",
        "🚔 थाना Sikandra Rao",
        "🚔 थाना Hasayan",
        "🚔 थाना Sahpau",
        "🚔 थाना Chandpa",
        "🚔 थाना Mahila, Hathras",
        "🚔 चौकी Chamar Gate",
        "🚔 चौकी Agra Road",
        "🚔 चौकी Mendu Gate",
        "🚔 GRP Hathras City"
    )

    val THANA_CUG_LIST = listOf(
        "थाना Hathras (Kotwali)" to "9454403578",
        "थाना Hathras Gate" to "9454403579",
        "थाना Hathras Junction" to "9454403580",
        "थाना Mursan" to "9454403581",
        "थाना Sasni" to "9454403584",
        "थाना Sadabad" to "9454403582",
        "थाना Sikandra Rao" to "9454403585",
        "थाना Hasayan" to "9454403577",
        "थाना Sahpau" to "9454403583",
        "थाना Chandpa" to "9454403576",
        "थाना Mahila" to "9454404868",
        "GRP Hathras City" to "9454404455"
    )

    val CO_LIST = listOf(
        "CO City (Hathras)" to "9454401258",
        "CO Sadabad" to "9454401259",
        "CO Sikandra Rao" to "9454401260"
    )

    const val DEFAULT_ASP_NUMBER = "9454401064"
    const val DEFAULT_SP_NUMBER = "9454400278"

    private val NEAR_THANA_MAP = mapOf(
        "Mendu Gate, Hathras" to "थाना Hathras (Kotwali)",
        "Chamar Gate, Hathras" to "थाना Hathras (Kotwali)",
        "Agra Road, Hathras" to "थाना Hathras (Kotwali)",
        "Sadabad Gate, Hathras" to "थाना Hathras (Kotwali)",
        "🚔 चौकी Chamar Gate" to "थाना Hathras (Kotwali)",
        "🚔 चौकी Agra Road" to "थाना Hathras (Kotwali)",
        "🚔 चौकी Mendu Gate" to "थाना Hathras (Kotwali)",
        "Hathras Junction" to "थाना Hathras Junction",
        "Mursan" to "थाना Mursan",
        "Sasni" to "थाना Sasni",
        "Sadabad" to "थाना Sadabad",
        "Sikandra Rao" to "थाना Sikandra Rao",
        "Hasayan" to "थाना Hasayan",
        "Sahpau" to "थाना Sahpau",
        "Chandpa" to "थाना Chandpa"
    )

    fun resolveThanaNumber(location: String): String {
        THANA_CUG_LIST.firstOrNull { location.contains(it.first) }?.let { return it.second }
        val mappedThana = NEAR_THANA_MAP[location]
        if (mappedThana != null) {
            THANA_CUG_LIST.firstOrNull { it.first == mappedThana }?.let { return it.second }
        }
        return "9454403578" // Default to Kotwali
    }

    fun resolveCONumber(thanaNumber: String): String {
        return when (thanaNumber) {
            "9454403578", "9454403579" -> "9454401258" // CO City
            "9454403582", "9454403576", "9454403583" -> "9454401259" // CO Sadabad
            "9454403580", "9454403585", "9454403577" -> "9454401260" // CO Sikandra Rao
            else -> "9454401258"
        }
    }

    fun getTemplateItems(type: EmergencyType): List<ChecklistItem> {
        val rawItems = when (type) {
            EmergencyType.FIRE -> listOf(
                "Fire Station को सूचना (101)" to "101",
                "Ambulance को सूचना (108)" to "108",
                "संबंधित थाने को सूचना" to "thana",
                "CO को सूचना" to "co",
                "ASP / वरिष्ठ अधिकारी को सूचना" to "asp",
                "SP को सूचना" to "sp",
                "घटना की Location दर्ज" to "",
                "सूचना का समय दर्ज" to "",
                "Fire Service/दमकल की पहुँच की पुष्टि" to "101",
                "क्षेत्र खाली कराया / बिजली-गैस बंद" to ""
            )
            EmergencyType.ACCIDENT -> listOf(
                "Ambulance को सूचना (108)" to "108",
                "संबंधित थाने को सूचना" to "thana",
                "CO को सूचना" to "co",
                "ASP / वरिष्ठ अधिकारी को सूचना" to "asp",
                "घटना की Location दर्ज" to "",
                "सूचना का समय दर्ज" to "",
                "Ambulance की पहुँच की पुष्टि" to "108",
                "यातायात क्लियर / डायवर्ट किया" to "",
                "घायलों को अस्पताल भेजा" to ""
            )
            EmergencyType.STAMPEDE -> listOf(
                "संबंधित थाने को सूचना (अतिरिक्त बल)" to "thana",
                "Ambulance को सूचना (108)" to "108",
                "CO को सूचना" to "co",
                "ASP / वरिष्ठ अधिकारी को सूचना" to "asp",
                "SP को सूचना" to "sp",
                "घटना की Location दर्ज" to "",
                "सूचना का समय दर्ज" to "",
                "भीड़ नियंत्रित / निकासी मार्ग खुले" to "",
                "Ambulance की पहुँच की पुष्टि" to "108",
                "घायलों की गिनती दर्ज" to ""
            )
            EmergencyType.MEDICAL -> listOf(
                "Ambulance को सूचना (108)" to "108",
                "संबंधित थाने को सूचना" to "thana",
                "नज़दीकी अस्पताल को सूचना" to "",
                "घटना की Location दर्ज" to "",
                "सूचना का समय दर्ज" to "",
                "Ambulance की पहुँच की पुष्टि" to "108",
                "परिजनों को सूचना" to ""
            )
            EmergencyType.LAW_AND_ORDER -> listOf(
                "संबंधित थाने को सूचना" to "thana",
                "CO को सूचना" to "co",
                "ASP / वरिष्ठ अधिकारी को सूचना" to "asp",
                "SP को सूचना" to "sp",
                "घटना की Location दर्ज" to "",
                "सूचना का समय दर्ज" to "",
                "अतिरिक्त फोर्स की पहुँच की पुष्टि" to "",
                "क्षेत्र सील / शांति वार्ता" to "",
                "वीडियो/सबूत सुरक्षित" to "",
                "FIR/GD दर्ज" to ""
            )
            EmergencyType.OTHER -> listOf(
                "संबंधित थाने को सूचना" to "thana",
                "CO को सूचना" to "co",
                "ASP / वरिष्ठ अधिकारी को सूचना" to "asp",
                "घटना की Location दर्ज" to "",
                "सूचना का समय दर्ज" to "",
                "टीम मौके पर पहुँची की पुष्टि" to ""
            )
        }

        return rawItems.mapIndexed { index, pair ->
            ChecklistItem(
                id = "item_${System.currentTimeMillis()}_$index",
                title = pair.first,
                contactKey = pair.second
            )
        }
    }
}
