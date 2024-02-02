package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    OrderVO getByIdWithDetail(Long id);

    PageResult pageQueryUser(int page, int pageSize, Integer status);

    void cancel(Long id);

    void getByIdAgain(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void rejection(OrdersRejectionDTO ordersConfirmDTO);

    void cancelAdmin(OrdersCancelDTO ordersConfirmDTO);

    void delivery(Long id);

    void complete(Long id);

    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;
}
