package com.lee.ioc.core;

import com.lee.ioc.bean.Bean;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2018/12/8 14:35
 */
public class ApplicationContext extends BeanFactoryImpl {

    private String fileName;

    public ApplicationContext(String fileName) {
        this.fileName = fileName;
    }

    public void init() {
        loadFile();
    }

    private void loadFile() {
        Yaml yaml = new Yaml();
        Optional.ofNullable(fileName)
                .filter(StringUtils::isNotBlank)
                .map(f -> Thread.currentThread().getContextClassLoader().getResourceAsStream(f))
                .map(is -> yaml.loadAs(is, Bean.class))
                .ifPresent(bean -> bean.getBean().forEach(this::registerBean));
    }
}
