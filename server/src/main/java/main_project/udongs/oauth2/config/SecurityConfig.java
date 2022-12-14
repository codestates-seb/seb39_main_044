package main_project.udongs.oauth2.config;

import lombok.AllArgsConstructor;
import main_project.udongs.oauth2.api.repository.UserRefreshTokenRepository;
import main_project.udongs.oauth2.oauth.entity.RoleType;
import main_project.udongs.oauth2.oauth.exception.RestAuthenticationEntryPoint;
import main_project.udongs.oauth2.oauth.filter.TokenAuthenticationFilter;
import main_project.udongs.oauth2.oauth.handler.OAuth2AuthenticationFailureHandler;
import main_project.udongs.oauth2.oauth.handler.OAuth2AuthenticationSuccessHandler;
import main_project.udongs.oauth2.oauth.handler.TokenAccessDeniedHandler;
import main_project.udongs.oauth2.oauth.repository.OAuth2AuthorizationRequestBasedOnCookieRepository;
import main_project.udongs.oauth2.oauth.service.CustomOAuth2UserService;
import main_project.udongs.oauth2.oauth.service.CustomUserDetailsService;
import main_project.udongs.oauth2.oauth.token.AuthTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    private final CorsProperties corsProperties;
    private final AppProperties appProperties;
    private final AuthTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oAuth2UserService;
    private final TokenAccessDeniedHandler tokenAccessDeniedHandler;
    private final UserRefreshTokenRepository userRefreshTokenRepository;

    /*
     * UserDetailsService ??????
     * */
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder());
//    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .headers().frameOptions().disable()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .accessDeniedHandler(tokenAccessDeniedHandler)
                .and()
                .authorizeRequests()
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .antMatchers("/login").permitAll()
                .antMatchers("/member/signup").permitAll()
                .antMatchers("/register").permitAll()
                .antMatchers("/register/**").permitAll()
                .antMatchers("/refresh").permitAll()
                .antMatchers("/h2/**").permitAll()
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .antMatchers("/api.html").permitAll()
                .antMatchers("/todo/**").hasAnyAuthority(RoleType.USER.getCode())
                .antMatchers("/board/**").hasAnyAuthority(RoleType.USER.getCode())
                .antMatchers("/user/**").hasAnyAuthority(RoleType.USER.getCode())
                .antMatchers("/api/**/admin/**").hasAnyAuthority(RoleType.ADMIN.getCode())
                .antMatchers("/ws-stomp", "/ws-stomp/**").permitAll() // stomp
                .antMatchers("app/**","/queue/**", "/topic/**").permitAll() // stomp
                .antMatchers("/chat/**","/chat/room/").permitAll() // stomp
                .antMatchers("/room.html", "/templates/**", "/webjars/**", "/favicon/**","/templates/chat/**","/resources/**").permitAll() // stomp
//                .anyRequest().authenticated()
                .anyRequest().permitAll()
                .and()
                .oauth2Login()
                .authorizationEndpoint()
                .baseUri("/oauth2/authorization")
                .authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository())
                .and()
                .redirectionEndpoint()
                .baseUri("/*/oauth2/code/*")
                .and()
                .userInfoEndpoint()
                .userService(oAuth2UserService)
                .and()
                .successHandler(oAuth2AuthenticationSuccessHandler())
                .failureHandler(oAuth2AuthenticationFailureHandler());

        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /*
     * auth ????????? ??????
     * */
//    @Override
//    @Bean(BeanIds.AUTHENTICATION_MANAGER)
//    protected AuthenticationManager authenticationManager() throws Exception {
//        return super.authenticationManager();
//    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /*
     * security ?????? ???, ????????? ????????? ??????
     * */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * ?????? ?????? ??????
     * */
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    /*
     * ?????? ?????? ?????? Repository
     * ?????? ????????? ?????? ?????? ????????? ??? ??????.
     * */
    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }

    /*
     * Oauth ?????? ?????? ?????????
     * */
    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler(
                tokenProvider,
                appProperties,
                userRefreshTokenRepository,
                oAuth2AuthorizationRequestBasedOnCookieRepository()
        );
    }

    /*
     * Oauth ?????? ?????? ?????????
     * */
    @Bean
    public OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
        return new OAuth2AuthenticationFailureHandler(oAuth2AuthorizationRequestBasedOnCookieRepository());
    }

    /*
     * Cors ??????
     * */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource corsConfigSource = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedHeaders(Arrays.asList(corsProperties.getAllowedHeaders().split(",")));
        corsConfig.setAllowedMethods(Arrays.asList(corsProperties.getAllowedMethods().split(",")));
//        corsConfig.setAllowedOrigins(Arrays.asList(corsProperties.getAllowedOrigins().split(",")));
        corsConfig.addAllowedOriginPattern("*");
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(corsConfig.getMaxAge());

        corsConfigSource.registerCorsConfiguration("/**", corsConfig);
        return corsConfigSource;
    }
}
