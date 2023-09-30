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
 *  RestClientException.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.exceptions;

/**
 * RestClientException exception is sent when some REST call fails.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@SuppressWarnings("serial")
public class RestClientException extends RuntimeException {

    public static final Integer STATUS_JSON_CONVERSION = 7;
    public static final Integer JWT_MALFORMED = 8;
    public static final Integer GENERATE_SSO_COOKIE = 9;

    private Integer status;

    public RestClientException(Integer status, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
    }
    
    public RestClientException(Integer status, Throwable causa) {
        super(causa);
        this.status = status;
    }
    
    public RestClientException(Integer status) {
        this.status = status;
    }

    public RestClientException(Integer status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
    
    @Override
    public String toString() {
    	return "Rest exception status:" + status + " message :" + getMessage();
    }
}
