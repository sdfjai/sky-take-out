package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {
    void insert(Orders orders);

    @Select("select * from orders where id=#{id}")
    Orders getOrderById(Long id);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    void update(Orders orders);

    @Select("select count(id) from orders where status=#{status}")
    Integer getStatus(Integer status);

    @Select("select * from orders where number=#{orderNumber}")
    Orders getNumber(String orderNumber);
}
