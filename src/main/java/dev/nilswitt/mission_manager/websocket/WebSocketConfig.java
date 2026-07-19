package dev.nilswitt.mission_manager.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Exposes a STOMP endpoint at {@code /api/ws} (already permitted in {@link
 * dev.nilswitt.mission_manager.security.SecurityConfig}) that any StompJS-compatible client can
 * connect to. Entity change notifications are broadcast on {@code /topic/entities/{EntityName}},
 * and mission log book (update) changes are additionally broadcast on {@code
 * /topic/missions/{missionId}}, by {@link EntityChangeBroadcaster}. {@link
 * StompAuthChannelInterceptor} authenticates the STOMP session on {@code CONNECT} and requires
 * VIEW permission on the mission for {@code SUBSCRIBE} to {@code /topic/missions/{missionId}}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
