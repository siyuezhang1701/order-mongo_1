package com.thoughtworks.ketsu.domain.user;


import com.mongodb.*;
import com.sun.org.apache.xpath.internal.operations.Or;
import com.thoughtworks.ketsu.domain.order.Order;
import com.thoughtworks.ketsu.domain.order.OrderItem;
import com.thoughtworks.ketsu.domain.product.Product;
import com.thoughtworks.ketsu.domain.product.ProductRepository;
import com.thoughtworks.ketsu.infrastructure.records.Record;
import com.thoughtworks.ketsu.web.jersey.Routes;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class User implements Record{
    private String id;
    private String name;

    public User(BasicDBObject obj){
        this.id = obj.getObjectId("_id").toString();
        this.name = obj.getString("name");
    }

    @Inject
    DB db;

    @Inject
    ProductRepository productRepository;


    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<Order> createOrder(Map<String, Object> info){
        String id = saveOrder(info);
        BasicDBObject obj = findOrderById(id);
        if(obj == null)
            return Optional.of(null);
        else {
            return Optional.of(dbobjToOrder(obj));
        }
    }

    public List<Order> listOrders() {
        DBCollection table = db.getCollection("orders");
        BasicDBObject document = new BasicDBObject();
        document.put("user_id", id);
        DBCursor cursor = table.find();
        List<Order> orderList = new ArrayList<>();
        while (cursor.hasNext()){
            orderList.add(dbobjToOrder((BasicDBObject) cursor.next()));
        }
        return orderList;
    }

    public Optional<Order> findOrder(String id){
        BasicDBObject obj = findOrderById(id);
        if(obj == null)
            return Optional.of(null);
        else {
            return Optional.of(dbobjToOrder(obj));
        }
    }

    @Override
    public Map<String, Object> toRefJson(Routes routes) {
        return toJson(routes);
    }

    @Override
    public Map<String, Object> toJson(Routes routes) {
        return new HashMap<String, Object>(){{
            put("id", String.valueOf(id));
            put("uri", routes.userUri(User.this));
            put("name", name);
        }};
    }

    private String saveOrder(Map<String, Object> info){
        double total_price = 0;

        BasicDBObject document = new BasicDBObject();
        document.put("user_id", id);
        document.put("name", info.get("name"));
        document.put("phone", info.get("phone"));
        document.put("address", info.get("address"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) info.get("order_items");
        for (Map<String, Object> item : items){
            double price = productRepository.find(item.get("product_id").toString()).get().getPrice();
            item.put("amount", price * Integer.valueOf(item.get("quantity").toString()));
            total_price += price;
        }
        document.put("total_price", total_price);
        document.put("order_items", items);

        DBCollection table = db.getCollection("orders");
        table.insert(document);
        return document.get("_id").toString();
    }

    private BasicDBObject findOrderById(String id){
        BasicDBObject document = new BasicDBObject();
        document.put("_id", new ObjectId(id));
       return  (BasicDBObject) db.getCollection("orders").findOne(document);
    }

    private Order dbobjToOrder(BasicDBObject obj){
        ObjectId objectId = obj.getObjectId("_id");
        Date date = objectId.getDate();
        Map<String, Object> map = obj.toMap();

        List<Map> orderItems = (List) map.get("order_items");
        List<OrderItem> orderItemList = orderItems.stream().map(
                item -> new OrderItem(
                        item.get("product_id").toString(),
                        Integer.valueOf(item.get("quantity").toString()),
                        Double.valueOf(item.get("amount").toString())
                )).collect(Collectors.toList());
        Order order = new Order(map.get("_id").toString(),
                map.get("user_id").toString(),
                map.get("name").toString(),
                map.get("address").toString(),
                map.get("phone").toString(),
                Double.valueOf(map.get("total_price").toString()),
                date,
                orderItemList
        );

        return order;
    }


}
