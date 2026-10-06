package app.cloudfit.billing.application.port.input

import app.cloudfit.billing.domain.CatalogView

interface GetCatalogUseCase {
    fun execute(country: String?): CatalogView
}
