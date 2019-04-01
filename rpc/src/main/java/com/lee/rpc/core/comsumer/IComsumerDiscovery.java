package com.lee.rpc.core.comsumer;

import java.util.List;

/**
 * 服务发现接口
 * @author lichujun
 * @date 2019/3/30 17:40
 */
public interface IComsumerDiscovery {

    List<String> discovery();
}
