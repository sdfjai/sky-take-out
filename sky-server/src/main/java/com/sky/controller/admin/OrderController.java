package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "订单管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/statistics")
    @ApiOperation("各状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    @ApiOperation("查询订单")
    @GetMapping("/details/{id}")
    public Result<OrderVO> getById(@PathVariable("id") Long id) {
        log.info("订单id: {}", id);
        OrderVO orderVO = orderService.getByIdWithDetail(id);
        return Result.success(orderVO);
    }

    @ApiOperation("接单")
    @PutMapping("/confirm")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    @ApiOperation("拒单")
    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersConfirmDTO) {
        orderService.rejection(ordersConfirmDTO);
        return Result.success();
    }

    @ApiOperation("取消订单")
    @PutMapping("/cancel")
    public Result cancel(@RequestBody OrdersCancelDTO ordersConfirmDTO) {
        orderService.cancelAdmin(ordersConfirmDTO);
        return Result.success();
    }

    @ApiOperation("派送订单")
    @PutMapping("/delivery/{id}")
    public Result delivery(@PathVariable("id") Long id) {
        log.info("订单id: {}", id);
        orderService.delivery(id);
        return Result.success();
    }

    @ApiOperation("完成订单")
    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable("id") Long id) {
        log.info("订单id: {}", id);
        orderService.complete(id);
        return Result.success();
    }
}
