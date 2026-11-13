package com.rescuevpn.app.data.model

/**
 * Curated provider information for the in-app free/static-IP finder.
 * Entries are informational only; R&MA never creates accounts or provisions servers.
 */
data class FreeStaticIpProvider(
    val id: String,
    val name: String,
    val status: String,
    val summary: String,
    val ipBehavior: String,
    val limitations: String,
    val officialUrl: String,
    val searchTerms: List<String>
)

object FreeStaticIpProviderCatalog {
    // Last checked against provider documentation: 2026-11-14.
    val providers = listOf(
        FreeStaticIpProvider(
            id = "oracle-always-free",
            name = "Oracle Cloud Always Free",
            status = "رایگان مشروط",
            summary = "ماشین مجازی Always Free به‌همراه امکان تخصیص IPv4 عمومی؛ مناسب‌ترین گزینهٔ فعلی برای آزمایش شخصی WireGuard.",
            ipBehavior = "IPv4 عمومی نمونهٔ E2.1.Micro تا زمانی که همان نمونه و تخصیص IP برقرار باشد قابل استفاده است؛ این IP رزروشدهٔ قابل‌انتقال و تضمین‌شده پس از حذف/تعویض VM نیست.",
            limitations = "ظرفیت منطقهٔ خانگی ممکن است موجود نباشد؛ ثبت‌نام و تأیید هویت/کارت ممکن است لازم باشد؛ منابع Always Free کم‌استفاده ممکن است طبق سیاست Oracle بازیابی شوند. شرایط و هزینهٔ ترافیک را پیش از ساخت VM بررسی کنید.",
            officialUrl = "https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm",
            searchTerms = listOf("oracle", "oci", "always free", "wireguard", "ipv4", "vps", "رایگان", "اوراکل", "آی پی ثابت")
        ),
        FreeStaticIpProvider(
            id = "google-cloud",
            name = "Google Cloud Free Tier",
            status = "برای IPv4 ثابت رایگان نیست",
            summary = "طرح رایگان VM دارد، اما IPv4 عمومی متصل به VM معمولی مشمول هزینهٔ ساعتی است؛ بنابراین گزینهٔ رایگانِ IP ثابت محسوب نمی‌شود.",
            ipBehavior = "Google امکان رزرو IPv4 ثابت را می‌دهد، ولی IP عمومی در حال استفاده روی VM استاندارد طبق جدول رسمی قیمت‌گذاری هزینه دارد.",
            limitations = "پیش از ایجاد هر منبع، قیمت منطقه و هزینهٔ IP و ترافیک را بررسی کنید. این مورد در نتایج گزینه‌های رایگان پیشنهادی قرار نمی‌گیرد.",
            officialUrl = "https://cloud.google.com/vpc/pricing",
            searchTerms = listOf("google", "gcp", "static ip", "ipv4", "cloud", "رایگان", "گوگل")
        )
    )
}
