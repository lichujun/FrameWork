package com.lee.ioc.core;

import com.lee.ioc.bean.Bean;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import java.util.Optional;

/**
 * yaml依赖注入
 * @author lichujun
 * @date 2018/12/8 14:35
 */
public class YamlAppContext extends BeanFactoryImpl {

    private String fileName;

    public YamlAppContext(String fileName) {
        this.fileName = fileName;
    }

    public void init() {
        loadFile();
    }

    /** 加载yaml文件，并进行依赖注入 */
    private void loadFile() {
        Yaml yaml = new Yaml();
        Optional.ofNullable(fileName)
                .filter(StringUtils::isNotBlank)
                // 将yaml文件转换到流
                .map(f -> Thread.currentThread().getContextClassLoader().getResourceAsStream(f))
                // 加载yaml文件
                .map(is -> yaml.loadAs(is, Bean.class))
                // 注册到容器
                .ifPresent(bean -> bean.getBean().forEach(this::registerBean));
    }
}
