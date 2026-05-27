package site.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/index").setViewName("forward:/index.html");
        registry.addViewController("/privacy").setViewName("forward:/privacy.html");
        registry.addViewController("/offer").setViewName("forward:/offer.html");
        registry.addViewController("/return-policy").setViewName("forward:/return-policy.html");
    }
}
