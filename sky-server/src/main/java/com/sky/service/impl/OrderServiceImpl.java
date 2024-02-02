package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONObjectCodec;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;

    private WebSocketClient webSocketClient;

    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        /*
        * 处理可能的异常情况
        */
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if (shoppingCarts==null||shoppingCarts.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //插入订单信息
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(orders.PENDING_PAYMENT);
        orders.setPayStatus(orders.UN_PAID);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orderMapper.insert(orders);
        //批量插入n条订单详情信息
        List<OrderDetail> orderDetails=new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        for (ShoppingCart cart : shoppingCarts) {
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        //删除购物车数据
        shoppingCartMapper.delete(userId);
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber()).build();
        return orderSubmitVO;
    }

    //根据id查询订单
    public OrderVO getByIdWithDetail(Long id) {
        OrderVO orderVO = new OrderVO();
        Orders order = orderMapper.getOrderById(id);
        List<OrderDetail> orderDetails=orderDetailMapper.list(id);
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    //查询历史订单
    public PageResult pageQueryUser(int page, int pageSize, Integer status) {
        PageHelper.startPage(page,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> orders=orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList=new ArrayList<>();
        if (orders!=null&&orders.getTotal()>0){
            for (Orders order : orders) {
                Long id = order.getId();
                List<OrderDetail> orderDetails=orderDetailMapper.list(id);
                OrderVO orderVO=new OrderVO();
                BeanUtils.copyProperties(order,orderVO);
                orderVO.setOrderDetailList(orderDetails);
                orderVOList.add(orderVO);
            }
        }
        return new PageResult(orders.getTotal(), orderVOList);
    }

    //取消订单
    public void cancel(Long id) {
        Orders orderById = orderMapper.getOrderById(id);
        if (orderById==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer status = orderById.getStatus();
        if (status>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(id);
        if (status.equals(Orders.TO_BE_CONFIRMED)){
            //退款处理(未实现)
            orders.setPayStatus(Orders.REFUND);
        }
        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    //再来一单
    public void getByIdAgain(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.list(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    //订单搜索
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.getStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.getStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.getStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }


    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = orderMapper.getOrderById(ordersConfirmDTO.getId());
        order.setStatus(Orders.CONFIRMED);
        orderMapper.update(order);
    }


    public void rejection(OrdersRejectionDTO ordersConfirmDTO) {
        Orders orderById = orderMapper.getOrderById(ordersConfirmDTO.getId());
        if (orderById.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            Orders order = Orders.builder()
                    .id(ordersConfirmDTO.getId())
                    .rejectionReason(ordersConfirmDTO.getRejectionReason())
                    .status(Orders.CANCELLED)
                    .payStatus(Orders.REFUND)
                    .build();
            if (orderById.getPayStatus()==Orders.PAID){
                order.setPayStatus(Orders.REFUND);
            }
            orderMapper.update(order);
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }


    public void cancelAdmin(OrdersCancelDTO ordersConfirmDTO) {
        Orders orderById = orderMapper.getOrderById(ordersConfirmDTO.getId());
        if (orderById.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            Orders order = Orders.builder()
                    .id(ordersConfirmDTO.getId())
                    .rejectionReason(ordersConfirmDTO.getCancelReason())
                    .status(Orders.CANCELLED)
                    .payStatus(Orders.REFUND)
                    .build();
            if (orderById.getPayStatus()==Orders.PAID){
                order.setPayStatus(Orders.REFUND);
            }
            orderMapper.update(order);
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }


    public void delivery(Long id) {
        Orders orderById = orderMapper.getOrderById(id);
        if (orderById!=null&&orderById.getStatus()==Orders.CONFIRMED){
            Orders order = Orders.builder()
                    .id(id)
                    .status(Orders.DELIVERY_IN_PROGRESS)
                    .build();
            orderMapper.update(order);
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }


    public void complete(Long id) {
        Orders orderById = orderMapper.getOrderById(id);
        if (orderById!=null&&orderById.getStatus()==Orders.DELIVERY_IN_PROGRESS){
            Orders order = Orders.builder()
                    .id(id)
                    .status(Orders.COMPLETED)
                    .build();
            orderMapper.update(order);
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }


    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception{
        Long currentId = BaseContext.getCurrentId();
        User user = userMapper.getId(currentId);
        //本来应该调用微信支付接口，实现交易
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code")!=null&&jsonObject.getString("code").equals("ORDERP")){
            throw new OrderBusinessException("该订单已支付");
        }
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        Orders order = orderMapper.getNumber(ordersPaymentDTO.getOrderNumber());
        if (order.getPayStatus().equals(Orders.UN_PAID)) {
            Orders od = Orders.builder()
                    .id(order.getId())
                    .status(Orders.TO_BE_CONFIRMED)
                    .payMethod(ordersPaymentDTO.getPayMethod())
                    .payStatus(Orders.PAID)
                    .build();
            orderMapper.update(od);
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId",order.getId());
        map.put("content","订单号"+ordersPaymentDTO.getOrderNumber());
        String s = JSONObject.toJSONString(map);
        //自定义一个WebSocket发送
        //webSocketServer.sendToAllClient(s);
        return vo;
    }
}
