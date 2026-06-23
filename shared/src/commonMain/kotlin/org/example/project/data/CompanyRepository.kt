package org.example.project.data

import org.example.project.domain.SgMediaCompany
import org.example.project.domain.model.CompanyProfile

/** Stores the editable issuer (company) profile in the key/value settings table. */
class CompanyRepository(private val settings: SettingsRepository) {

    suspend fun load(): CompanyProfile {
        val d = SgMediaCompany
        fun opt(v: String?): String? = v?.takeIf { it.isNotBlank() }
        return CompanyProfile(
            name = settings.get(K_NAME) ?: d.name,
            legalForm = opt(settings.get(K_FORM)) ?: d.legalForm,
            rc = opt(settings.get(K_RC)) ?: d.rc,
            nif = opt(settings.get(K_NIF)) ?: d.nif,
            nis = opt(settings.get(K_NIS)) ?: d.nis,
            articleImposition = opt(settings.get(K_ART)) ?: d.articleImposition,
            address = settings.get(K_ADDR) ?: d.address,
            city = settings.get(K_CITY) ?: d.city,
            wilaya = opt(settings.get(K_WILAYA)) ?: d.wilaya,
            phone = opt(settings.get(K_PHONE)) ?: d.phone,
            email = opt(settings.get(K_EMAIL)) ?: d.email,
            iban = opt(settings.get(K_IBAN)) ?: d.iban,
            bankName = opt(settings.get(K_BANK)) ?: d.bankName,
            footerNote = opt(settings.get(K_FOOTER)) ?: d.footerNote,
            defaultVatPct = settings.get(K_VAT)?.toDoubleOrNull() ?: d.defaultVatPct,
            vatExempt = settings.get(K_EXEMPT)?.toBooleanStrictOrNull() ?: d.vatExempt,
        )
    }

    suspend fun save(p: CompanyProfile) {
        settings.set(K_NAME, p.name)
        settings.set(K_FORM, p.legalForm ?: "")
        settings.set(K_RC, p.rc ?: "")
        settings.set(K_NIF, p.nif ?: "")
        settings.set(K_NIS, p.nis ?: "")
        settings.set(K_ART, p.articleImposition ?: "")
        settings.set(K_ADDR, p.address)
        settings.set(K_CITY, p.city)
        settings.set(K_WILAYA, p.wilaya ?: "")
        settings.set(K_PHONE, p.phone ?: "")
        settings.set(K_EMAIL, p.email ?: "")
        settings.set(K_IBAN, p.iban ?: "")
        settings.set(K_BANK, p.bankName ?: "")
        settings.set(K_FOOTER, p.footerNote ?: "")
        settings.set(K_VAT, p.defaultVatPct.toString())
        settings.set(K_EXEMPT, p.vatExempt.toString())
    }

    private companion object {
        const val K_NAME = "company_name"
        const val K_FORM = "company_form"
        const val K_RC = "company_rc"
        const val K_NIF = "company_nif"
        const val K_NIS = "company_nis"
        const val K_ART = "company_article"
        const val K_ADDR = "company_address"
        const val K_CITY = "company_city"
        const val K_WILAYA = "company_wilaya"
        const val K_PHONE = "company_phone"
        const val K_EMAIL = "company_email"
        const val K_IBAN = "company_iban"
        const val K_BANK = "company_bank"
        const val K_FOOTER = "company_footer"
        const val K_VAT = "company_vat"
        const val K_EXEMPT = "company_vatexempt"
    }
}
