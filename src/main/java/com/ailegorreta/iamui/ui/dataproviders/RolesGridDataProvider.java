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
 *  RolesGridDataProvider.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.dataproviders;

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
import com.ailegorreta.iamui.backend.data.dto.facultad.RolDTO;
import com.ailegorreta.iamui.backend.data.service.RolService;

/**
 * A pageable Roles data provider.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@SuppressWarnings("serial")
@SpringComponent
@UIScope
public class RolesGridDataProvider extends FilterablePageableDataProvider<RolDTO, RolFilter> {
	public static final String[] ROL_SORT_FIELDS = {"nombre"};

	private final RolService 			service;
	private List<QuerySortOrder> 		defaultSortOrders;
	private Consumer<Page<RolDTO>> 		pageObserver;
	
	@Autowired
	public RolesGridDataProvider(RolService service) {
		this.service = service;
		setSortOrders(Sort.Direction.ASC, ROL_SORT_FIELDS);
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
	protected Page<RolDTO> fetchFromBackEnd(Query<RolDTO, RolFilter> query, Pageable pageable) {
		RolFilter 		filter = query.getFilter().orElse(RolFilter.getEmptyFilter());
		Page<RolDTO> 	page = service.findAnyMatchingActive(Optional.ofNullable(filter), pageable);
		
		if (pageObserver != null) 
			pageObserver.accept(page);
		
		return page;
	}

	@Override
	protected List<QuerySortOrder> getDefaultSortOrders() {
		return defaultSortOrders;
	}

	@Override
	protected int sizeInBackEnd(Query<RolDTO, RolFilter> query) {
		RolFilter filter = query.getFilter().orElse(RolFilter.getEmptyFilter());
		
		return service.countAnyMatchingActive(Optional.ofNullable(filter)).block().intValue();
	}

	public void setPageObserver(Consumer<Page<RolDTO>> pageObserver) {
		this.pageObserver = pageObserver;
	}

	@Override
	public Object getId(RolDTO item) {
		return item.getId();
	}
}
