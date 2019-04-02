package com.lee.rpc.core;

import java.util.List;

/**
 * 服务发现接口
 * @author lichujun
 * @date 2019/3/30 17:40
 */
public interface IProviderDiscovery {

    List<String> discovery();
}
