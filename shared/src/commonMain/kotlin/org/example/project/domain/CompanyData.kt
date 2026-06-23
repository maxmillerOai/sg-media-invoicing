package org.example.project.domain

import org.example.project.domain.model.CompanyProfile

/**
 * SG Media's own issuer profile — used as the header/footer of every invoice and
 * administrative document. Source of truth for the company's legal identifiers.
 */
val SgMediaCompany = CompanyProfile(
    name = "SG MEDIA PROD",
    legalForm = "EURL",
    rc = "16/00.104866 B 20",
    nif = "002016104896603",
    nis = "002016010256068",
    address = "Coop immobilière familiale local 06/lot 224 Sec. Grp de pt 1358 RDC. Bordj-El-Bahri",
    city = "Alger",
    phone = "+213 (0)660 53 53 00",
)

/** "EURL SG MEDIA PROD" */
val CompanyProfile.legalName: String
    get() = listOfNotNull(legalForm, name).joinToString(" ")

/** "RC: …  •  NIF: …  •  NIS: …" */
val CompanyProfile.legalIdsLine: String
    get() = listOfNotNull(
        rc?.let { "RC : $it" },
        nif?.let { "NIF : $it" },
        nis?.let { "NIS : $it" },
        articleImposition?.let { "Art. : $it" },
    ).joinToString("   •   ")

/** "address, city" */
val CompanyProfile.addressLine: String
    get() = listOfNotNull(address.ifBlank { null }, city.ifBlank { null }).joinToString(", ")
