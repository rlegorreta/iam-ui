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
 *  RolFilter.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.dataproviders;

import java.io.Serializable;

/**
 * A pageable Roles data provider filter.
 *
 * This is declared outside RolesGridDataProvider because the RolesEditor
 * is a different class.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
public class RolFilter implements Serializable {
	private static final long serialVersionUID = 5743727761700179533L;
	
	private String 	filter;
	private boolean showActive;

	public RolFilter(String filter, boolean showActive) {
		this.filter = filter;
		this.showActive = showActive;
	}

	public String getFilter() {
		return filter;
	}

	public boolean isShowActive() {
		return showActive;
	}
	
	public static RolFilter getEmptyFilter() {
		return new RolFilter("", false);
	}
}
