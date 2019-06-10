package cn.wishhust.muxin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
//扫描mybatis mapper包路径
@MapperScan(basePackages = "cn.wishhust.muxin.mapper")
// 扫描 所有需要的包, 包含一些自用的工具类包 所在的路径
@ComponentScan(basePackages = {"cn.wishhust.muxin","org.n3r.idworker"})
public class MuxinApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuxinApplication.class, args);
    }

}
