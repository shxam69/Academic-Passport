package com.academicpassport;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class BeanCheckRunner implements CommandLineRunner {
    private final ApplicationContext ctx;
    public BeanCheckRunner(ApplicationContext ctx) { this.ctx = ctx; }
    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== BEAN LIST ======");
        for (String beanName : ctx.getBeanDefinitionNames()) {
            if (beanName.toLowerCase().contains("flyway")) {
                System.out.println("FOUND FLYWAY BEAN: " + beanName);
            }
        }
        System.out.println("=======================");
    }
}
