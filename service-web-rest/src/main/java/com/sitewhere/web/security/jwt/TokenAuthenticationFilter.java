/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.security.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sitewhere.microservice.security.InvalidJwtException;
import com.sitewhere.microservice.security.JwtExpiredException;
import com.sitewhere.security.SitewhereGrantedAuthority;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.microservice.multitenant.InvalidTenantException;
import com.sitewhere.spi.microservice.multitenant.TenantEngineNotAvailableException;
import com.sitewhere.spi.microservice.security.ITokenManagement;
import com.sitewhere.spi.security.ITenantAwareAuthentication;
import com.sitewhere.spi.tenant.ITenant;
import com.sitewhere.spi.user.IGrantedAuthority;
import com.sitewhere.web.security.SiteWhereHttpHeaders;
import com.sitewhere.web.spi.microservice.IWebRestMicroservice;

import io.jsonwebtoken.Claims;

/**
 * Filter that pulls JWT and tenant token from authentication header and pushes
 * it into Spring {@link SecurityContextHolder}.
 * 
 * @author Derek
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    /** Static logger instance */
    private static Log LOGGER = LogFactory.getLog(TokenAuthenticationFilter.class);

    /** Microservice */
    private IWebRestMicroservice<?> microservice;

    /** Authentication manager */
    private AuthenticationManager authenticationManager;

    public TokenAuthenticationFilter(IWebRestMicroservice<?> microservice,
	    AuthenticationManager authenticationManager) {
	this.microservice = microservice;
	this.authenticationManager = authenticationManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.filter.OncePerRequestFilter#doFilterInternal(
     * javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
	    throws IOException, ServletException {

	String jwt = SiteWhereHttpHeaders.getJwtFromHeader(request);
	String tenantId = SiteWhereHttpHeaders.getTenantIdFromHeader(request);
	String tenantAuth = SiteWhereHttpHeaders.getTenantAuthFromHeader(request);
	if (jwt != null) {
	    // Get username from token and load user.
	    ITokenManagement tokenManagement = getMicroservice().getTokenManagement();
	    try {
		Claims claims = tokenManagement.getClaimsForToken(jwt);
		String username = tokenManagement.getUsernameFromClaims(claims);
		LOGGER.debug("JWT decoded for username: " + username);
		List<IGrantedAuthority> auths = tokenManagement.getGrantedAuthoritiesFromClaims(claims);
		List<GrantedAuthority> springAuths = new ArrayList<GrantedAuthority>();
		for (IGrantedAuthority auth : auths) {
		    springAuths.add(new SitewhereGrantedAuthority(auth));
		}

		// Create authentication object based on JWT and tenant token.
		JwtAuthenticationToken token = new JwtAuthenticationToken(username, springAuths, jwt);
		Authentication authenticated = getAuthenticationManager().authenticate(token);
		if ((!StringUtils.isEmpty(tenantId)) && (StringUtils.isEmpty(tenantAuth))) {
		    throw new SiteWhereException("Tenant id passed without corresponding tenant auth token.");
		}

		// Add tenant authentication data if provided.
		addTenantAuthenticationData(authenticated, tenantId, tenantAuth);

		SecurityContextHolder.getContext().setAuthentication(authenticated);
		LOGGER.debug("Added authentication to context.");
		chain.doFilter(request, response);
	    } catch (JwtExpiredException e) {
		LOGGER.debug("Expired JWT passed.", e);
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "JWT has expired.");
	    } catch (InvalidJwtException e) {
		LOGGER.debug("Invalid JWT passed.", e);
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "JWT is invalid.");
	    } catch (TenantEngineNotAvailableException e) {
		LOGGER.debug("Requested tenant engine was not available.", e);
		response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Tenant engine not available.");
	    } catch (InvalidTenantException e) {
		LOGGER.debug(e.getMessage(), e);
		response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
	    } catch (Throwable e) {
		LOGGER.error("Unhandled exception in token authentication filter.", e);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing JWT.");
	    }
	} else {
	    chain.doFilter(request, response);
	}
    }

    /**
     * Based on fields passed in HTTP headers, look up tenant and verify that tenant
     * auth token is valid. Store tenant information in Spring authentication data
     * so that it can be passed via GRPC channels for remote microservices.
     * 
     * @param authenticated
     * @param tenantToken
     * @param tenantAuth
     * @throws SiteWhereException
     */
    protected void addTenantAuthenticationData(Authentication authenticated, String tenantToken, String tenantAuth)
	    throws SiteWhereException {
	if ((authenticated instanceof ITenantAwareAuthentication) && (tenantToken != null) && (tenantAuth != null)) {
	    // Load tenant using superuser credentials.
	    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
	    try {
		SecurityContextHolder.getContext()
			.setAuthentication(getMicroservice().getSystemUser().getAuthentication());
		ITenant tenant = getMicroservice().getCachedTenantManagement().getTenantByToken(tenantToken);
		if (tenant == null) {
		    throw new InvalidTenantException("The requested tenant does not exist.");
		} else if (!tenant.getAuthenticationToken().equals(tenantAuth)) {
		    throw new InvalidTenantException("Tenant authentication token is invalid.");
		}
		((ITenantAwareAuthentication) authenticated).setTenant(tenant);
		LOGGER.debug("Added tenant to authentication: " + tenant.getId());
	    } finally {
		SecurityContextHolder.getContext().setAuthentication(previous);
	    }
	}
    }

    protected IWebRestMicroservice<?> getMicroservice() {
	return microservice;
    }

    protected void setMicroservice(IWebRestMicroservice<?> microservice) {
	this.microservice = microservice;
    }

    protected AuthenticationManager getAuthenticationManager() {
	return authenticationManager;
    }

    protected void setAuthenticationManager(AuthenticationManager authenticationManager) {
	this.authenticationManager = authenticationManager;
    }
}