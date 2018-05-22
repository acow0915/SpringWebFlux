package com.tw.web.flux;

import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tw.web.flux.jwt.JWTAuthorizationWebFilter;
import com.tw.web.flux.jwt.WebFilterChainServerJWTAuthenticationSuccessHandler;

import reactor.core.publisher.Mono;

@SpringBootApplication
@ComponentScan(basePackages = {"com.tw.web.flux"})
@RestController
public class WebFluxApp {

	public static void main(String[] args) {
		SpringApplication.run(WebFluxApp.class, args);
	}
	
	@GetMapping("testing")
	public Mono<String> testing(){
		return Mono.just("hellow world");
	}
	
	@GetMapping("/")
	public Mono<Map<String, String>> hello(Mono<Principal> principal) {
		return principal
			.map(Principal::getName)
			.map(this::helloMessage);
	}

	private Map<String, String> helloMessage(String username) {
		return Collections.singletonMap("message", "Hello " + username + "!");
	}
}

class JdbcReactiveUserDetailsService implements ReactiveUserDetailsService {
	
	private DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	@Override
	public Mono<UserDetails> findByUsername(String username) {
		try {
			PreparedStatement stmt = dataSource.getConnection().prepareStatement("select * from user where name='" + username + "'");
			ResultSet rs = stmt.executeQuery();
			if(null != rs) {
				UserDetails userDetails = null;
				while (rs.next()) {
					userDetails = User.withDefaultPasswordEncoder()
							.username(rs.getString("name"))
							.password(rs.getString("password"))
							.roles("USER")
							.build();
				}
				return Mono.just(User.withUserDetails(userDetails).build());
			} else {
				return Mono.empty();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Mono.empty();
	}
}

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {
	
	@Autowired
	private DataSource dataSource;
	
	//IN MEMORY DEMO
	/*
	@Bean
	public MapReactiveUserDetailsService userDetailsService() {
		
		UserDetails userDetails = User.withDefaultPasswordEncoder()
		.username("Tim")
		.password("123qwe")
		.roles("USER")
		.build();
		return new MapReactiveUserDetailsService(userDetails);
	}
	*/
	
	@Bean
	public JdbcReactiveUserDetailsService userDetailsService() {
		JdbcReactiveUserDetailsService userDetailsService = new JdbcReactiveUserDetailsService();
		userDetailsService.setDataSource(dataSource);
		return userDetailsService;
	}
	
	/**
	 * JWT
	 * @param http
	 * @return
	 */
	@Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		AuthenticationWebFilter authenticationJWT;
		authenticationJWT = new AuthenticationWebFilter(new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService()));
		authenticationJWT.setAuthenticationSuccessHandler(new WebFilterChainServerJWTAuthenticationSuccessHandler());
		http
        .authorizeExchange()
        .pathMatchers("/login", "/")
        .permitAll()
        .and()
        .addFilterAt(authenticationJWT, SecurityWebFiltersOrder.FIRST)
        .authorizeExchange()
        .pathMatchers("/**")
        .authenticated()
        .and()
        .addFilterAt(new JWTAuthorizationWebFilter(), 
        		SecurityWebFiltersOrder.HTTP_BASIC);

		return http.build();
	}
}
