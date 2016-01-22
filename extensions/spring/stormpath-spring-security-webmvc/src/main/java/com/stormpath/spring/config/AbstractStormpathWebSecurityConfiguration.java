/*
 * Copyright 2015 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.spring.config;

import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.idsite.IdSiteResultListener;
import com.stormpath.sdk.saml.SamlResultListener;
import com.stormpath.sdk.servlet.csrf.CsrfTokenManager;
import com.stormpath.sdk.servlet.csrf.DisabledCsrfTokenManager;
import com.stormpath.sdk.servlet.http.Saver;
import com.stormpath.sdk.servlet.mvc.ErrorModelFactory;
import com.stormpath.spring.csrf.SpringSecurityCsrfTokenManager;
import com.stormpath.spring.security.provider.SpringSecurityIdSiteResultListenerSpringSecurity;
import com.stormpath.spring.security.provider.SpringSecuritySamlResultListenerSpringSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;


/**
 * @since 1.0.RC5
 */
@Order(99)
public abstract class AbstractStormpathWebSecurityConfiguration {

    @Autowired
    protected Client client;

    @Autowired
    protected AuthenticationManager authenticationManager;

    @Autowired(required = false) //required = false when stormpath.web.enabled = false
    @Qualifier("stormpathAuthenticationResultSaver")
    protected Saver<AuthenticationResult> authenticationResultSaver; //provided by stormpath-spring-webmvc

    @Value("#{ @environment['stormpath.web.login.uri'] ?: '/login' }")
    protected String loginUri;

    @Value("#{ @environment['stormpath.web.login.nextUri'] ?: '/' }")
    protected String loginNextUri;

    @Value("#{ @environment['stormpath.web.csrf.token.enabled'] ?: true }")
    protected boolean csrfTokenEnabled;

    @Value("#{ @environment['stormpath.web.csrf.token.name'] ?: '_csrf'}")
    protected String csrfTokenName;

    public StormpathWebSecurityConfigurer stormpathWebSecurityConfigurer() {
        return new StormpathWebSecurityConfigurer();
    }

    public AuthenticationSuccessHandler stormpathAuthenticationSuccessHandler() {
        StormpathLoginSuccessHandler loginSuccessHandler = new StormpathLoginSuccessHandler(client, authenticationResultSaver);
        loginSuccessHandler.setDefaultTargetUrl(loginNextUri);
        return loginSuccessHandler;
    }

    public AuthenticationFailureHandler stormpathAuthenticationFailureHandler() {
        String loginFailureUri = loginUri + "?error";
        SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler(loginFailureUri);
        handler.setAllowSessionCreation(false); //not necessary
        return handler;
    }

    public ErrorModelFactory stormpathLoginErrorModelFactory() {
        return new SpringSecurityLoginErrorModelFactory();
    }

    public LogoutHandler stormpathLogoutHandler() {
        return new StormpathLogoutHandler(authenticationResultSaver);
    }

    public IdSiteResultListener springSecurityIdSiteResultListener() {
        return new SpringSecurityIdSiteResultListenerSpringSecurity(authenticationManager);
    }

    public SamlResultListener springSecuritySamlResultListener() {
        return new SpringSecuritySamlResultListenerSpringSecurity(authenticationManager);
    }

    public CsrfTokenRepository stormpathCsrfTokenRepository() {
        HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
        csrfTokenRepository.setParameterName(csrfTokenName);
        return csrfTokenRepository;
    }

    public CsrfTokenManager stormpathCsrfTokenManager() {
        //Spring Security supports CSRF protection only in Thymeleaf or JSP's with Sec taglib., therefore we
        //cannot just delegate the CSRF strategy to Spring Security, we need to handle it ourselves in Spring.
        if (csrfTokenEnabled) {
            return new SpringSecurityCsrfTokenManager(stormpathCsrfTokenRepository(), csrfTokenName);
        }
        return new DisabledCsrfTokenManager(csrfTokenName);

    }

}
