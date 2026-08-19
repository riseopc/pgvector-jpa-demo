package org.riseopc.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogEnvComponent implements CommandLineRunner {

    private final Environment environment;

    @Override
    public void run(String... args) throws Exception {
        log.warn("======spring.datasource.url=>{}", environment.getProperty("spring.datasource.url"));
        log.warn("======spring.datasource.username=>{}", environment.getProperty("spring.datasource.username"));
    }

}
