/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  CompaniasGridDataProvider.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.dataproviders

import com.ailegorreta.iamui.backend.data.dto.compania.CompaniaDTO
import com.ailegorreta.iamui.backend.data.dto.compania.Negocio
import com.ailegorreta.iamui.backend.data.service.CompaniaService
import com.ailegorreta.client.dataproviders.FilterablePageableHierarchicalDataProvider
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.QuerySortOrderBuilder
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.io.Serializable
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * A pageable Companias data provider in Kotlin.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@SpringComponent
@UIScope
class CompaniasGridDataProvider: FilterablePageableHierarchicalDataProvider<CompaniaDTO, CompaniaFilter> {

    companion object {
        val COMPANIA_SORT_FIELDS = arrayOf("nombre")
    }

    private val service: CompaniaService
    private val defaultSortOrders: List<QuerySortOrder>
    private var pageObserver:Consumer<Page<CompaniaDTO>>? = null

    @Autowired
    constructor(service: CompaniaService) {
        this.service = service
        defaultSortOrders = setSortOrders(Sort.Direction.ASC, COMPANIA_SORT_FIELDS)
    }

    private fun setSortOrders(direction: Sort.Direction , properties: Array<String>): List<QuerySortOrder> {
        val builder =  QuerySortOrderBuilder()

        properties.forEach {
            if (direction.isAscending())
                builder.thenAsc(it)
            else
                builder.thenDesc(it)
        }

        return builder.build();
    }

    override fun getDefaultSortOrders(): List<QuerySortOrder> {
        return defaultSortOrders
    }

    override fun fetchFromBackEnd(query: HierarchicalQuery<CompaniaDTO, CompaniaFilter>, pageable: Pageable): Page<CompaniaDTO> {
        val filter: CompaniaFilter = query.filter.orElse(CompaniaFilter.emptyFilter)
        val page:Page<CompaniaDTO> = service.findAnyMatchingNegocio(Optional.ofNullable(filter), pageable)

        if (pageObserver != null)
            pageObserver!!.accept(page)

        return page
    }

    override fun getId(item: CompaniaDTO): Long {
        return item.id!!
    }

    override fun hasChildren(companiaDTO: CompaniaDTO): Boolean {
        return companiaDTO.areas.isNotEmpty()
    }

    override fun getChildCount(query: HierarchicalQuery<CompaniaDTO, CompaniaFilter>): Int {
        val filterQuery = super.getFilterQuery(query)

        if (filterQuery.parent == null) {
            val filter: CompaniaFilter = filterQuery.getFilter().orElse(CompaniaFilter.emptyFilter)
            val count = service.countAnyMatchingNegocio(Optional.ofNullable(filter)).block()!! as Long

            return count.toInt()
        } else
            return filterQuery.parent.areas.size
    }

    override fun fetchChildrenFromBackEnd(query: HierarchicalQuery<CompaniaDTO, CompaniaFilter>): Stream<CompaniaDTO> {
        val filterQuery = super.getFilterQuery(query)

        if (filterQuery.parent == null) {
            val pageable = getPageable(filterQuery)
            val result: Page<CompaniaDTO> = fetchFromBackEnd(filterQuery, pageable)

            return fromPageable(result, pageable, filterQuery)
        } else {
            /* no need to go to backend the children are in companiaDTO.fondos just convert them */
            var fondos = ArrayList<CompaniaDTO>()

            query.parent.areas.forEach { fondos.add(CompaniaDTO.fromEntity(it)) }

            return fondos.stream()
        }
    }
}

data class CompaniaFilter constructor (val filter: String = "", val negocio: Negocio = Negocio.TODOS): Serializable {
    companion object {
        val  emptyFilter = CompaniaFilter()
    }

}
