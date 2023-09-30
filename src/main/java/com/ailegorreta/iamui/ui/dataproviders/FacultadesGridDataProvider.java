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
 *  FacultadesGridDataProvider.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.dataproviders;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.vaadin.artur.spring.dataprovider.FilterablePageableDataProvider;

import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.QuerySortOrderBuilder;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import com.ailegorreta.iamui.backend.data.dto.facultad.FacultadDTO;
import com.ailegorreta.iamui.backend.data.service.FacultadService;

/**
 * A pageable facultades data provider.
 *
 *  @project : IAM-UI
 *  @author rlh
 *  @date February 2022
 */
@SpringComponent
@UIScope
public class FacultadesGridDataProvider extends FilterablePageableDataProvider<FacultadDTO, FacultadesGridDataProvider.FacultadFilter> {
	public static final String[] FACULTAD_SORT_FIELDS = {"nombre"};

	public static class FacultadFilter implements Serializable {
		private final String  filter;
		private final boolean showActive;

		public FacultadFilter(String filter, boolean showActive) {
			this.filter = filter;
			this.showActive = showActive;
		}

		public String getFilter() {
			return filter;
		}

		public boolean isShowActive() {
			return showActive;
		}
		
		public static FacultadFilter getEmptyFilter() {
			return new FacultadFilter("", false);
		}
	}

	private final FacultadService 		facultadService;
	private List<QuerySortOrder> 		defaultSortOrders;
	private Consumer<Page<FacultadDTO>> pageObserver;
	
	@Autowired
	public FacultadesGridDataProvider(FacultadService facultadService) {
		this.facultadService = facultadService;
		setSortOrders(Sort.Direction.ASC, FACULTAD_SORT_FIELDS);
	}

	private void setSortOrders(Sort.Direction direction, String[] properties) {
		QuerySortOrderBuilder builder = new QuerySortOrderBuilder();
		
		for (String property : properties) {
			if (direction.isAscending()) 
				builder.thenAsc(property);
			else 
				builder.thenDesc(property);
		}
		defaultSortOrders = builder.build();
	}	

	@Override
	protected Page<FacultadDTO> fetchFromBackEnd(Query<FacultadDTO, FacultadFilter> query, Pageable pageable) {
		FacultadFilter 		filter = query.getFilter().orElse(FacultadFilter.getEmptyFilter());
		Page<FacultadDTO> 	page = facultadService.findAnyMatchingActive(Optional.ofNullable(filter), pageable);
		
		if (pageObserver != null) 
			pageObserver.accept(page);
		
		return page;
	}

	@Override
	protected List<QuerySortOrder> getDefaultSortOrders() {
		return defaultSortOrders;
	}

	@Override
	protected int sizeInBackEnd(Query<FacultadDTO, FacultadFilter> query) {
		FacultadFilter filter = query.getFilter().orElse(FacultadFilter.getEmptyFilter());

		var res = facultadService.countAnyMatchingActive(Optional.of(filter)).block();

		return res.intValue();
	}

	public void setPageObserver(Consumer<Page<FacultadDTO>> pageObserver) {
		this.pageObserver = pageObserver;
	}

	@Override
	public Object getId(FacultadDTO item) {
		return item.getId();
	}
}
