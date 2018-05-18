package com.tw.web.flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebFilter;

import com.tw.web.flux.service.UserHandler;

@Configuration
public class RouterConfig {

	/**
	 * reflectionUtils 方式
	 * @param userHandler
	 * @return
	 */
//	@SuppressWarnings("unchecked")
//	@Bean
//    @Autowired
//    public RouterFunction<ServerResponse> routerFunctionReflection(final UserHandler userHandler) {
//		
//        return RouterFunctions.route(RequestPredicates.path("/user"), request -> {
//                System.out.println(request.queryParam("action").get());
//        	return request.queryParam("action").map(action -> 
//                		Mono.justOrEmpty(
//                				ReflectionUtils.findMethod(UserHandler.class, action, ServerRequest.class))
//                				.flatMap(method -> (Mono<ServerResponse>)ReflectionUtils.invokeMethod(method, userHandler, request))
//                				.switchIfEmpty(ServerResponse.badRequest().build())
//                				.onErrorResume(ex -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
//                				.orElse(ServerResponse.badRequest().build());
//        }			
//        		);	
//        
//    }
	
	
	@Bean
    @Autowired
    public RouterFunction<ServerResponse> routerFunction(final UserHandler userHandler) {
		return RouterFunctions.route(RequestPredicates.GET("/user/listUsers"), userHandler::listUsers)
						.andRoute(RequestPredicates.GET("/user/getUserByName"), userHandler::getUserByName);
		
//        return RouterFunctions.route(RequestPredicates.GET("/listUsers"), request -> {
//                System.out.println(request.queryParam("action").get());
//        	return request.queryParam("action").map(action -> 
//                		Mono.justOrEmpty(
//                				ReflectionUtils.findMethod(UserHandler.class, action, ServerRequest.class))
//                				.flatMap(method -> (Mono<ServerResponse>)ReflectionUtils.invokeMethod(method, userHandler, request))
//                				.switchIfEmpty(ServerResponse.badRequest().build())
//                				.onErrorResume(ex -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
//                				.orElse(ServerResponse.badRequest().build());
//        }			
//        		);	
        
    }
	
	@Bean
	public HttpHandler getHttpHandler(RouterFunction<ServerResponse> routes, WebFilter springSecurityFilterChain) {
		HandlerStrategies strategies = HandlerStrategies.builder().webFilter(springSecurityFilterChain).build();
		return RouterFunctions.toHttpHandler(routes, strategies);
	}
}
