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
 *  PerfilesGridDataProvider.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.dataproviders

import com.ailegorreta.iamui.backend.data.dto.facultad.PerfilDTO
import com.ailegorreta.iamui.backend.data.service.PerfilService
import com.vaadin.flow.data.provider.Query
import com.vaadin.flow.data.provider.QuerySortOrder
import com.vaadin.flow.data.provider.QuerySortOrderBuilder
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.vaadin.artur.spring.dataprovider.FilterablePageableDataProvider
import java.io.Serializable
import java.util.*
import java.util.function.Consumer

/**
 * A pageable Perfiles data provider in Kotlin.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@SpringComponent
@UIScope
class PerfilesGridDataProvider: FilterablePageableDataProvider<PerfilDTO, PerfilFilter> {
	companion object {
		val PERFIL_SORT_FIELDS = arrayOf("nombre")
	}

	private val service: PerfilService
	private val defaultSortOrders: List<QuerySortOrder>
	private var pageObserver:Consumer<Page<PerfilDTO>>? = null
		
	constructor(service: PerfilService) {
		this.service = service
		defaultSortOrders = setSortOrders(Sort.Direction.ASC, PERFIL_SORT_FIELDS)
	}

	private fun setSortOrders(direction: Sort.Direction , properties: Array<String>): List<QuerySortOrder> {
		val builder: QuerySortOrderBuilder =  QuerySortOrderBuilder()
		
		properties.forEach {
			if (direction.isAscending())
				builder.thenAsc(it)
			else
				builder.thenDesc(it)
		}

		return builder.build();
	}
	
	override fun getDefaultSortOrders() = defaultSortOrders
	
	override fun fetchFromBackEnd(query: Query<PerfilDTO, PerfilFilter>, pageable: Pageable): Page<PerfilDTO> {
		val filter: PerfilFilter = query.getFilter().orElse(PerfilFilter.emptyFilter)
		val page:Page<PerfilDTO> = service.findAnyMatchingActive(Optional.ofNullable(filter), pageable)
		
		if (pageObserver != null)
			pageObserver!!.accept(page)
		
		return page	
	}
	
	override fun sizeInBackEnd(query: Query<PerfilDTO, PerfilFilter>): Int {
		val filter: PerfilFilter = query.getFilter().orElse(PerfilFilter.emptyFilter)
		
		return service.countAnyMatchingActive(Optional.ofNullable(filter)).block()!!.toInt()
	}

	override fun getId(item: PerfilDTO) = item.id
}

data class PerfilFilter constructor (val filter: String = "", val showActive: Boolean = false): Serializable {
	companion object {
		val  emptyFilter = PerfilFilter()
	}
}
