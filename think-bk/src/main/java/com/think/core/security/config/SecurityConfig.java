package com.think.core.security.config;

import com.think.core.security.filter.TokenAuthenticationFilter;
import com.think.core.security.handler.AccessDeniedHandlerImpl;
import com.think.core.security.handler.AuthenticationEntryPointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;


/**
 * Security 配置
 * @author 好市民
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private TokenAuthenticationFilter tokenAuthenticationFilter;
    @Autowired
    private AccessDeniedHandlerImpl accessDeniedHandler;
    @Autowired
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        httpSecurity
                .csrf().disable()
                .cors(Customizer.withDefaults())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authorizeRequests()
                // 可匿名访问的接口配置
                .antMatchers(
                        // 登录
                        "/auth/login",
                        // 上传凭证
                        "/os/uploadToken",
                        // 微信消息推送（GET 配置校验 + POST 事件推送）：微信不带 token，
                        // 安全性靠报文上的 signature 与 wechat.notify-token 比对，不靠登录态
                        "/wechat/notify",
                        // 运营数据统计看板（think-admin）：无管理员登录，上来即可看数据，
                        // 全部只读 GET，不暴露写接口，故整组 permitAll
                        "/stats/**"
                ).permitAll()
                // 其余接口需要认证
                .anyRequest().authenticated();

        httpSecurity.exceptionHandling().accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint);
        httpSecurity.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
