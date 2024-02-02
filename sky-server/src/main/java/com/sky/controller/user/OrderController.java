package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "C端-订单接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @ApiOperation("用户下单")
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单参数: {}",ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    @ApiOperation("用户支付")
    @PutMapping("/payment")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception{
        log.info("用户支付参数: {}",ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        return Result.success(orderPaymentVO);
    }

    @ApiOperation("查询订单")
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> getById(@PathVariable("id") Long id){
        log.info("订单id: {}",id);
        OrderVO orderVO = orderService.getByIdWithDetail(id);
        return Result.success(orderVO);
    }

    @ApiOperation(" 查询历史订单")
    @GetMapping("/historyOrders")
    public Result<PageResult> page(int page, int pageSize, Integer status){
        PageResult pageResult=orderService.pageQueryUser(page,pageSize,status);
        return Result.success(pageResult);
    }

    @ApiOperation("取消订单")
    @PutMapping("/cancel/{id}")
    public Result cancelOrder(@PathVariable("id") Long id){
        orderService.cancel(id);
        return Result.success();
    }

    @ApiOperation("再来一单")
    @PostMapping("/repetition/{id}")
    public Result OrderAgain(@PathVariable("id") Long id){
        orderService.getByIdAgain(id);
        return Result.success();
    }
}
