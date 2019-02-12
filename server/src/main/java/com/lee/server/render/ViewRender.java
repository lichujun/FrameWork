package com.lee.server.render;

import com.lee.server.bean.ModelAndView;
import com.lee.server.core.ApplicationContext;
import com.lee.server.core.RequestHandlerChain;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author lichujun
 * @date 2018/12/15 14:26
 */
@Slf4j
public class ViewRender implements Render {
    private ModelAndView mv;
    public ViewRender(Object mv) {
        if (mv instanceof ModelAndView) {
            this.mv = (ModelAndView) mv;
        } else if (mv instanceof String) {
            this.mv = new ModelAndView().setView((String) mv);
        } else {
            throw new RuntimeException("返回类型不合法");
        }
    }
    @Override
    public void render(RequestHandlerChain handlerChain) throws Exception {
        HttpServletRequest req = handlerChain.getRequest();
        HttpServletResponse resp = handlerChain.getResponse();
        String path = mv.getView();
        Map<String, Object> model = mv.getModel();
        model.forEach(req::setAttribute);
        req.getRequestDispatcher(ApplicationContext.getCONFIGURATION().getViewPath() + path).forward(req, resp);
    }
}
