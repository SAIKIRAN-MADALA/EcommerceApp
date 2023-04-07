package com.gproject.OrderService.service;

import com.gproduct.GoProduct.model.ProductResponse;
import com.gproduct.PaymentService.model.PaymentMode;
import com.gproduct.PaymentService.model.PaymentResponse;
import com.gproject.OrderService.entity.Order;
import com.gproject.OrderService.exception.CustomException;
import com.gproject.OrderService.external.client.PaymentService;
import com.gproject.OrderService.external.client.ProductService;
import com.gproject.OrderService.external.client.request.PaymentRequest;
import com.gproject.OrderService.model.OrderRequest;
import com.gproject.OrderService.model.OrderResponse;
import com.gproject.OrderService.repository.OrderRepository;
import org.aspectj.weaver.ast.Or;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderServiceImplTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    OrderServiceImpl orderService = new OrderServiceImpl();

    @BeforeEach
    void setUp() {
    }

    @DisplayName("Get Order - Success Scenario")
    @Test
    void test_When_Order_Success() {
        //Mock internal calls
        Order order =  getMockOrder();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/"+order.getProductId(),
                ProductResponse.class
        )).thenReturn(getMockProductResponse());

        when(restTemplate
                .getForObject("http://PAYMENT-SERVICE/payment/order/`"+order.getId(), PaymentResponse.class))
                .thenReturn(getMockPaymentResponse());
        //Call Actual Methods
        OrderResponse orderResponse = orderService.getOrderDetails(1);
        //Verify
        verify(orderRepository, times(1)).findById(anyLong());
        verify(restTemplate,times(1)).getForObject( "http://PRODUCT-SERVICE/product/"+order.getProductId(),
                ProductResponse.class);
        verify(restTemplate,times(1)).getForObject( "http://PAYMENT-SERVICE/payment/order/`"+order.getId(), PaymentResponse.class);
        //Assert
        assertNotNull(orderResponse);
        assertEquals( orderResponse.getOrderId() ,orderResponse.getOrderId());
    }

    @DisplayName("Get Order - Failure Scenario")
    @Test
    void test_When_Get_Order_NOT_FOUND_then_Not_Found(){

        when(orderRepository.findById(anyLong())).thenReturn(Optional.ofNullable(null));

        CustomException customException = assertThrows(CustomException.class, ()-> orderService.getOrderDetails(1) );
        assertEquals("NOT_FOUND", customException.getErrorCode());
        assertEquals(404, customException.getStatus());

        verify(orderRepository, times(1)).findById(anyLong());
    }

    @DisplayName("Place order Success Scenario")
    @Test
    void test_When_Place_Order_Success(){
        Order order = getMockOrder();
        OrderRequest orderRequest = getMockOrderRequest();

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productService.reduceQuantity(anyLong(),anyLong())).thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        when(paymentService.doPayment(any(PaymentRequest.class))).thenReturn(new ResponseEntity<Long>(1L,HttpStatus.OK));

        long orderId = orderService.placeOrder(orderRequest);

        verify(orderRepository,times(2)).save(any());
        verify(productService,times(1)).reduceQuantity(anyLong(),anyLong());
        verify(paymentService,times(1)).doPayment(any(PaymentRequest.class));
        assertEquals(orderId, order.getId());
    }

    private OrderRequest getMockOrderRequest() {
        return OrderRequest.builder()
                .paymentMode(com.gproject.OrderService.model.PaymentMode.CASH)
                .productId(1)
                .totalAmount(200)
                .quantity(12)
                .build();
    }

    private PaymentResponse getMockPaymentResponse() {
        return PaymentResponse.builder()
                .paymentDate(Instant.now())
                .orderId(1)
                .paymentId(1222)
                .paymentMode(PaymentMode.CASH)
                .status("ACCEPTED")
                .amount(2000)
                .build();
    }

    private ProductResponse getMockProductResponse() {
        return ProductResponse.builder()
                .productName("iPhone")
                .price(2000)
                .productId(1)
                .quantity(2000)
                .build();
    }

    private Order getMockOrder() {
        return Order.builder()
                .orderStatus("PLACED")
                .productId(Long.parseLong("1"))
                .OrderDate(Instant.now())
                .id(1)
                .quantity(Long.parseLong("30"))
                .build();
    }

    @Test
    void getOrderDetails() {
    }
}