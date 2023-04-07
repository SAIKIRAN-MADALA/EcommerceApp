package com.gproject.OrderService.controller;

import com.gproject.OrderService.model.OrderRequest;
import com.gproject.OrderService.model.OrderResponse;
import com.gproject.OrderService.service.OrderService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@Log4j2
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/placeOrder")
    private ResponseEntity<Long> placeAnOrder(@RequestBody OrderRequest orderRequest){
        log.info("Placing an Order Request : {}",orderRequest);
        long id= orderService.placeOrder(orderRequest);
        log.info("Order Id: " +id);
        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable  long orderId){
        OrderResponse orderResponse =  orderService.getOrderDetails(orderId);
        return new ResponseEntity<>(orderResponse,HttpStatus.OK);
    }

}
