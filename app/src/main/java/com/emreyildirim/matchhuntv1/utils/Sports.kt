package com.emreyildirim.matchhuntv1.utils

import androidx.compose.ui.graphics.Color
import com.emreyildirim.matchhuntv1.R

data class SportInfo(
    val name: String,
    val nameEn: String,
    val iconResId: Int,
    val color: Color
)

object Sports {
    val football = SportInfo(
        name = "Futbol",
        nameEn = "Football",
        iconResId = R.drawable.football,
        color = Color(0xFF81C784) // Açık Yeşil
    )
    
    val basketball = SportInfo(
        name = "Basketbol",
        nameEn = "Basketball",
        iconResId = R.drawable.basketball,
        color = Color(0xFFFFB74D) // Açık Turuncu
    )
    
    val volleyball = SportInfo(
        name = "Voleybol",
        nameEn = "Volleyball",
        iconResId = R.drawable.volleyball,
        color = Color(0xFF4FC3F7) // Açık Mavi
    )
    
    val tennis = SportInfo(
        name = "Tenis",
        nameEn = "Tennis",
        iconResId = R.drawable.tennis,
        color = Color(0xFFDCE775) // Sarı-Yeşil
    )
    
    val swimming = SportInfo(
        name = "Yüzme",
        nameEn = "Swimming",
        iconResId = R.drawable.swimming,
        color = Color(0xFF4DD0E1) // Açık Turkuaz
    )
    
    val running = SportInfo(
        name = "Koşu",
        nameEn = "Running",
        iconResId = R.drawable.runner,
        color = Color(0xFF9575CD) // Mor-Mavi
    )
    
    val cycling = SportInfo(
        name = "Bisiklet",
        nameEn = "Cycling",
        iconResId = R.drawable.cycling,
        color = Color(0xFFE57373) // Açık Kırmızı
    )
    
    val yoga = SportInfo(
        name = "Yoga",
        nameEn = "Yoga",
        iconResId = R.drawable.yoga,
        color = Color(0xFFCE93D8) // Lavanta
    )
    
    val pilates = SportInfo(
        name = "Pilates",
        nameEn = "Pilates",
        iconResId = R.drawable.pilates,
        color = Color(0xFFF8BBD0) // Pudra
    )
    
    val fitness = SportInfo(
        name = "Fitness",
        nameEn = "Fitness",
        iconResId = R.drawable.weightlifter,
        color = Color(0xFFFF8A65) // Koyu Turuncu
    )
    
    val kickbox = SportInfo(
        name = "Kick Boks",
        nameEn = "Kickboxing",
        iconResId = R.drawable.kickbox,
        color = Color(0xFFA1887F) // Şarap Rengi
    )
    
    val boxing = SportInfo(
        name = "Boks",
        nameEn = "Boxing",
        iconResId = R.drawable.boxing,
        color = Color(0xFFEF5350) // Kırmıız
    )
    
    val tableTennis = SportInfo(
        name = "Masa Tenisi",
        nameEn = "Table Tennis",
        iconResId = R.drawable.tabeltennis,
        color = Color(0xFFB0BEC5) // Açık Gri Mavi
    )
    
    val badminton = SportInfo(
        name = "Badminton",
        nameEn = "Badminton",
        iconResId = R.drawable.badminton,
        color = Color(0xFFAED581) // Açık Yeşil
    )
    
    val golf = SportInfo(
        name = "Golf",
        nameEn = "Golf",
        iconResId = R.drawable.golf,
        color = Color(0xFF81C784) // Açık Yeşil
    )
    
    val judo = SportInfo(
        name = "Judo",
        nameEn = "Judo",
        iconResId = R.drawable.judo,
        color = Color(0xFFFFF176)
    )
    
    val karate = SportInfo(
        name = "Karate",
        nameEn = "Karate",
        iconResId = R.drawable.carate,
        color = Color(0xFFE0E0E0)
    )
    
    val taekwondo = SportInfo(
        name = "Taekwondo",
        nameEn = "Taekwondo",
        iconResId = R.drawable.carate,
        color = Color(0xFF90CAF9)
    )
    
    val archery = SportInfo(
        name = "Okçuluk",
        nameEn = "Archery",
        iconResId = R.drawable.archery,
        color = Color(0xFFF06292)
    )
    
    val skiing = SportInfo(
        name = "Kayak",
        nameEn = "Skiing",
        iconResId = R.drawable.skiing,
        color = Color(0xFF81D4FA)
    )
    
    val iceSkating = SportInfo(
        name = "Buz Pateni",
        nameEn = "Ice Skating",
        iconResId = R.drawable.iceskating,
        color = Color(0xFFB3E5FC)
    )
    
    val snowboarding = SportInfo(
        name = "Snowboard",
        nameEn = "Snowboarding",
        iconResId = R.drawable.snowboarding,
        color = Color(0xFFCFD8DC)
    )
    
    val rafting = SportInfo(
        name = "Rafting",
        nameEn = "Rafting",
        iconResId = R.drawable.rafting,
        color = Color(0xFF4FC3F7)
    )
    
    val rowing = SportInfo(
        name = "Kürek",
        nameEn = "Rowing",
        iconResId = R.drawable.rowing,
        color = Color(0xFF80CBC4)
    )

    val motocycle = SportInfo(
        name = "Motocycle",
        nameEn = "Motorcycle",
        iconResId = R.drawable.motocycle,
        color = Color(0xFF90A4AE)
    )

    val allSports = listOf(
        football, basketball, volleyball, tennis, swimming,
        running, cycling, motocycle, yoga, pilates, fitness, kickbox,
        boxing, tableTennis, badminton, golf, judo, karate,
        taekwondo, archery, skiing, iceSkating, snowboarding,
        rafting, rowing
    )

    // Eski list fonksiyonunu koruyalım (geriye dönük uyumluluk için)
    val list = allSports.map { it.name }

    // Spor türüne göre SportInfo döndüren yardımcı fonksiyon
    fun getSportInfo(sportName: String): SportInfo? {
        return allSports.find { it.name.equals(sportName, ignoreCase = true) || it.nameEn.equals(sportName, ignoreCase = true) }
    }
}