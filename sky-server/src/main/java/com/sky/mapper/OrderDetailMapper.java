package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    //批量插入
    void insertBatch(List<OrderDetail> orderDetails);

    @Select("select * from order_detail where order_id=#{id}")
    List<OrderDetail> list(Long id);

    @Select("select * from order_detail where order_id=#{id}")
    List<OrderDetail> getByOrderId(Long id);
}
