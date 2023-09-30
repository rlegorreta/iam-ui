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
 *  Role
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data;

/**
 * These are all permits that are allowed in this App.
 *
 * This is NOT an entity nor a DTO. This class is to define
 * all permits (facultades) to be mapped in each @Secure view
 * and also in the SecurityConfiguration class
 *
 * @author: rlh
 * @project iam-ui
 * @date: July 2023
 */
public class Role {
	public static final String ALL = "ROLE_ALL";
	public static final String ADMIN_IAM = "ROLE_ADMINIAM";

	public static final String USER_IAM = "ROLE_USERIAM";
	public static final String MASTER_ADMIN = "ROLE_MASTERADMIN";

	public static final String IAM_REPORTES = "ROLE_IAM_REPORTES";
	public static final String IAM_ESTADISTICAS = "ROLE_IAM_ESTADISTICAS";

	private Role() {
		// Static methods and fields only
	}

	public static String[] getAllRoles() {
		return new String[] { ALL, ADMIN_IAM, USER_IAM, MASTER_ADMIN};
	}

}
