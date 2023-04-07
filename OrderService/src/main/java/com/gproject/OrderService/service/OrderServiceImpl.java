package com.gproject.OrderService.service;

import com.gproduct.GoProduct.model.ProductResponse;
import com.gproduct.PaymentService.model.PaymentMode;
import com.gproduct.PaymentService.model.PaymentResponse;
import com.gproject.OrderService.entity.Order;
import com.gproject.OrderService.exception.CustomException;
import com.gproject.OrderService.external.client.PaymentService;
import com.gproject.OrderService.external.client.ProductService;
import com.gproject.OrderService.external.client.request.PaymentRequest;
import com.gproject.OrderService.model.ORDERSTATUS;
import com.gproject.OrderService.model.OrderRequest;
import com.gproject.OrderService.model.OrderResponse;
import com.gproject.OrderService.repository.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService {
    /**
     * @param orderRequest
     * @return
     */
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public long placeOrder(OrderRequest orderRequest) {

        log.info("Placing Order Request: {}",orderRequest);

        productService.reduceQuantity(orderRequest.getProductId(),orderRequest.getQuantity());

        log.info("Calling for Payment Service");

        Order order = Order.builder()
                .orderStatus(ORDERSTATUS.CREATED.toString())
                .amount(orderRequest.getTotalAmount())
                .productId(orderRequest.getProductId())
                .quantity(orderRequest.getQuantity())
                .OrderDate(Instant.now())
                .build();
        order = orderRepository.save(order);

        log.info("Calling Payment Service to complete the payment");

        PaymentRequest paymentRequest =  PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMode(PaymentMode.valueOf(orderRequest.getPaymentMode().name()))
                .amount(orderRequest.getTotalAmount())
                .build();
        String OrderStatus=null;
        try{
            paymentService.doPayment(paymentRequest);
            log.info("Placed the order successfully"+order.getId());
            OrderStatus="SUCCESS";
        }catch (Exception e){
            log.error("Failed to do the payment"+order.getId());
            OrderStatus="FAILED";
        }
        log.info("Creating Order with Status CREATED");
        order.setOrderStatus(OrderStatus);
        order = orderRepository.save(order);

        log.info("Order Placed Successfully : {}",order.getId());
        return order.getId();
    }

    /**
     * @param orderId
     * @return
     */
    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("Get order Details for Order Id: {}",orderId);

        Order order= orderRepository.findById(orderId)
                .orElseThrow(()-> new CustomException("Order Not Found for the order Id:"+orderId,"NOT_FOUND",404));

        log.info("Invoking Product service to fetch product details");
        ProductResponse productResponse = restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/"+order.getProductId(),
                ProductResponse.class
        );

        OrderResponse.ProductDetails productDetails = OrderResponse.ProductDetails.builder()
                .productName(productResponse.getProductName())
                .price(productResponse.getPrice())
                .productId(productResponse.getProductId())
                .quantity(productResponse.getQuantity())
                .build();
        log.info("Getting payment information for the payment service");

        PaymentResponse paymentResponse = restTemplate.getForObject("http://PAYMENT-SERVICE/payment/order/`"+order.getId(), PaymentResponse.class);

        OrderResponse.PaymentDetails paymentDetails = OrderResponse.PaymentDetails.builder()
                .paymentId(paymentResponse.getPaymentId())
                .status(paymentResponse.getStatus())
                .paymentMode(paymentResponse.getPaymentMode())
                .amount(paymentResponse.getAmount())
                .orderId(paymentResponse.getOrderId())
                .build();

        return OrderResponse.builder()
                .OrderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .orderId(order.getId() )
                .productDetails(productDetails)
                .paymentDetails(paymentDetails)
                .build();
    }
}
