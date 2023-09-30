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
 *  Application
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui;

import com.ailegorreta.iamui.backend.data.dto.Notification;
import com.ailegorreta.iamui.backend.service.MessageService;
import com.ailegorreta.iamui.config.ServiceConfig;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.client.RSocketGraphQlClient;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.web.client.RestTemplate;

/**
 * Spring boot web application initializer for the front UI of the IAM administration.
 *
 * @author rlh
 * @project : IAM-UI
 * @date September 2023
 */
@SpringBootApplication
// @NpmPackage(value = "lumo-css-framework", version = "^4.0.10")
@EnableDiscoveryClient
@Theme(value = "iam-ui")
@PWA(name = "IAM UI", shortName = "IAM", offlinePath="offline-page.html", offlineResources = { "./images/offline-login-banner.jpg"})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@ComponentScan(basePackages = {"com.ailegorreta.iamui", "com.ailegorreta.client.security"})
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public  PropertySourcesPlaceholderConfigurer propertyConfigurer() {
		PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();

		propertyConfigurer.setPlaceholderPrefix("@{");
		propertyConfigurer.setPlaceholderSuffix("}");
		propertyConfigurer.setIgnoreUnresolvablePlaceholders(true);

		return propertyConfigurer;
	}

	@Bean
	public  PropertySourcesPlaceholderConfigurer defaultPropertyConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	/**
	 * This RSocket is to handle notifications from the Audit microservice on-line.
	 *
	 * note: This socket does not the Resource protection in Spring Security. Use it with caution
	 *       (i.e., just for notifications)
	 * note: This RSocket does not use the Gateway the microservice is called directly
	 *
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public RSocketGraphQlClient rSocketGraphQlClient(RSocketGraphQlClient.Builder<?> builder,
													 ServiceConfig serviceConfig) {
		return builder.tcp(serviceConfig.getSubscriptionHost(), serviceConfig.getSubscriptionPort())
				      .route("graphql")
				      .build();
	}

	/**
	 * In this method we create a GraphQL subscription with the Auth microservice in order to receive on-line
	 * notifications.
	 *
	 * In order to push the notification the user must be subscribed and the destination must me de user or
	 * username = null for all users.
	 *
	 */
	@Bean
	ApplicationRunner applicationRunner(RSocketGraphQlClient rsocket,
										MessageService messageService) {
		return args -> {
			var rsocketRequestDocument = """
                    subscription {
                      notification { 
                          username
                          title
                          message
                      }
                    }
                    """;

			rsocket.document(rsocketRequestDocument)
					.retrieveSubscription("notification")
					.toEntity(Notification.class)
					.subscribe(notification -> messageService.sendNotification(notification.getTitle(),
							                                                   notification.getMessage(),
																			   notification.getUsername()));
		};
	}

}
