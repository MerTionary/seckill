package com.dapeng.seckill.controller;


import com.dapeng.seckill.bean.OrderInfo;
import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.GoodsService;
import com.dapeng.seckill.service.OrderService;
import com.dapeng.seckill.vo.GoodsVo;
import com.dapeng.seckill.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    GoodsService goodsService;

    @RequestMapping("/detail")
    public Result<OrderDetailVo> info(Model model, SeckillUser user,@RequestParam("orderId") long orderId){
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 获取订单信息
        OrderInfo order = orderService.getOrderById(orderId);
        if (order == null) {
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }

        // 如果订单存在，则根据订单信息获取商品信息
        long goodsId = order.getGoodsId();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setOrder(order);// 设置订单信息
        orderDetailVo.setGoods(goods);// 设置商品信息
        return Result.success(orderDetailVo);
    }

}