package com.example.config;

import com.example.security.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final JwtAuthInterceptor jwtAuthInterceptor;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path uploadsPath = Path.of(System.getProperty("user.dir"), "uploads");
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:" + uploadsPath.toAbsolutePath() + "/")
				.setCachePeriod(31536000);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(jwtAuthInterceptor)
				.addPathPatterns("/api/**")
				.excludePathPatterns(
						"/api/user/login",
						"/api/user/register",
						"/api/user/avatar/**"
				);
	}
}

