package com.weworklab.searcher2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import java.io.IOException;

@Controller
@Configuration
@SpringBootApplication
@EnableAutoConfiguration
public class Searcher2Application extends SpringBootServletInitializer {

    public static void main(String[] args) throws IOException, InterruptedException {

        ConfigurableApplicationContext context = SpringApplication.run(Searcher2Application.class, args);

        context.getBean(SourceUrls.class).init();
    }
}
