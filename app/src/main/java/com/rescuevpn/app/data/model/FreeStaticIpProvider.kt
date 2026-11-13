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
    val computeOptions: String,
    val ipBehavior: String,
    val limitations: String,
    val lastReviewed: String,
    val officialUrl: String,
    val setupGuideUrl: String? = null,
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
            computeOptions = "دو مسیر محاسباتی در سهمیهٔ رایگان: VM.Standard.E2.1.Micro (AMD، حدود 1 GB RAM و 1/8 OCPU) یا سهمیهٔ OCI Ampere A1 Flex (ARM، مجموعاً تا 2 OCPU و 12 GB RAM؛ قابل تقسیم بین VMها). هر دو برای اجرای WireGuard روی لینوکس قابل بررسی‌اند؛ سازگاری معماری image و اسکریپت نصب باید رعایت شود.",
            ipBehavior = "E2.1.Micro در مستندات رسمی یک IP عمومی و تا 50 Mbps پهنای باند اینترنت دارد. آدرس عمومی متصل به VM را IP رزروشدهٔ قابل‌انتقال و تضمین‌شده پس از حذف/تعویض VM فرض نکنید؛ نوع تخصیص را در کنسول بررسی کنید.",
            limitations = "ظرفیت منطقهٔ خانگی ممکن است موجود نباشد؛ ثبت‌نام و تأیید هویت/کارت ممکن است لازم باشد؛ منابع Always Free کم‌استفاده ممکن است طبق سیاست Oracle بازیابی شوند. شرایط و هزینهٔ ترافیک را پیش از ساخت VM بررسی کنید.",
            lastReviewed = "2026-11-14",
            officialUrl = "https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm",
            setupGuideUrl = "https://github.com/vahid250/rescue-vpn/blob/rebuild/rma-android-structure/docs/ORACLE_CUSTOMER_OWNED_WIREGUARD_GATEWAY_GUIDE.md",
            searchTerms = listOf("oracle", "oci", "always free", "wireguard", "ipv4", "vps", "رایگان", "اوراکل", "آی پی ثابت")
        ),
        FreeStaticIpProvider(
            id = "google-cloud",
            name = "Google Cloud Free Tier",
            status = "برای IPv4 ثابت رایگان نیست",
            summary = "طرح رایگان VM دارد، اما IPv4 عمومی متصل به VM معمولی مشمول هزینهٔ ساعتی است؛ بنابراین گزینهٔ رایگانِ IP ثابت محسوب نمی‌شود.",
            computeOptions = "Free Tier VM به‌تنهایی به معنی IPv4 عمومی رایگان نیست.",
            ipBehavior = "Google امکان رزرو IPv4 ثابت را می‌دهد، ولی IP عمومی در حال استفاده روی VM استاندارد طبق جدول رسمی قیمت‌گذاری هزینه دارد.",
            limitations = "پیش از ایجاد هر منبع، قیمت منطقه و هزینهٔ IP و ترافیک را بررسی کنید. این مورد در نتایج گزینه‌های رایگان پیشنهادی قرار نمی‌گیرد.",
            lastReviewed = "2026-11-14",
            officialUrl = "https://cloud.google.com/vpc/pricing",
            searchTerms = listOf("google", "gcp", "static ip", "ipv4", "cloud", "رایگان", "گوگل")
        )
    )
}
