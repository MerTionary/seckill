# Seckill 高并发高性能秒杀系统
SpringBoot-Seckill


1.用户登录

    用户输入的密码两次MD5计算后入库，
    第一次：表单密码=MD5(明文密码+salt)，固定盐值，前端js脚本计算。
    第二次：数据库密码=MD5(表单密码+salt)，随机盐值存在user数据库中，后端计算。
    
2.JSR303参数校验

    自定义@IsMobile注解，IsMobileValidator校验是否为手机号码。
  
3.GlobalExceptionHandler
    
    @ControllerAdvice 当将异常抛到controller时,可以对异常进行统一处理,规定返回的json格式 或 跳转到一个错误页面
    @ExceptionHandler(value = Exception.class) 这个注解用指定这个方法对何种异常处理（这里默认所有异常都用这个方法处理）
    全局异常处理器接管了所有的异常处理，无论何种异常全部抛给自定义的全局异常处理器去解决。
    用户登录, 要么处理成功返回true，否则会抛出全局异常，抛出的异常信息会被全局异常接收，
    全局异常会将异常信息传递到全局异常处理器，自定义的GlobalExceptionHandler默认会对所有的异常都进行处理。
   
4.Redis
    
    FastJson将对象序列化成json字符串，自己封装jedis。生成一个redisService，同时根据不同的数据加一个不同的prefix，以实现key的差异化。
    实现了redis数据的存入、取出、过期时间的设置，暂不使用SpringBoot提供的RedisTemplate。
    
5.分布式session
    
    在用户登录成功之后，后台生成一个类似JSEESSIONID的token来表示英语，通过response.addCookie通知客户端保存此cookie，
    浏览器的每次请求都在请求头中携带此token，然后后端根据token查询redis中是否有对应的user，由此实现了原生Session的功能。也减轻了数据库的压力。
    
6.UserArgumentResolver (注册在WebMvcConfigurer)

     当请求参数为SeckillUser时，使用这个解析器处理
     * 客户端的请求到达某个Controller的方法时，判断这个方法入参是否为SeckillUser，
     * 如果是，则这个SeckillUser参数对象通过下面的resolveArgument()方法获取。
     * 然后，该Controller方法继续往下执行时所看到的SeckillUser对象就是在这里的resolveArgument()方法处理过的对象。
     * 本来根据Cookie从Redis里获取User是在controller方法中做，现在将其封装为UserArgumentResolver，直接将user封装到在controller的入参中。
     * 且每次调用userService.getSeckillUserByToken()封装user时都同时更新cookie以延长session的有效期，这与原生seesion的超时销毁机制相同。
     
7.数据库设计

     * seckill_goods
     * seckill_order
     * seckill_user
     * order_info
     数据库连接池使用Druid，因为可以配置servlet实现sql监控。
     
8.页面设计

     * goodsList.html
     * goodDetail.html
     * ordeDetail.html
     使用模板引擎Thymeleaf、Bootstrap、Jquery
     
9.页面优化

     * 页面级缓存+URL缓存+对象缓存
     * 页面静态化、前后端分离
     * 静态资源优化
     * CDN优化
     
     页面级缓存+URL缓存+对象缓存
     
     所谓页面缓存，指的是对于服务端的请求，不直接从系统中获取页面资源，而是先从redis缓存中获取页面资源，如果缓存中不存在页面资源，则系统将渲染页面并存储页面到缓存中，然后将页面返回。适合数据基本无变化的页面。
     请求到服务端，服务端查询数据库中的商品列表信息然后存储在Model对象中，Thymeleaf页面获取在Model对象中的商品列表信息然后动态渲染，再返回给客户端。
     如果每次请求都做这样的工作，势必会对服务器和系统造成一定的压力（系统的压力主要来源于每次Thymeleaf页面获取在Model对象的信息都要渲染一次），所以可做一个页面级的缓存，减轻数据库和系统的压力。
     比如商品列表页：
       @RequestMapping(value = "/toList", produces = "text/html")
       从redis取出html缓存
       html缓存不为空，返回该html
       若html为空，查询秒杀商品列表，用于手动渲染时将商品数据填充到页面
       手动渲染页面 html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext)，并返回html。
       
     所谓URL缓存，实际上和页面缓存是一样的，在本项目中，我们对商品详情页做了缓存，商品详情页的请求需要goodsId，也就是说，对每一个goodsId对应的goodsList都做了一个缓存，
     其他的和商品列表页的缓存思路是一致的，只不过商品取详情页是需要动态的根据goodsId来取。
     
     通过上面的叙述可知，URL缓存和页面缓存的不同之处在于，URL缓存需要根据URL中的参数动态地取缓存，而页面缓存则不需要。
     URL缓存和页面缓存的缓存时间都比较短，在本项目中，我们设置商品详情页和商品列表页的缓存时间为60s。
       
     对象缓存是一种更细粒度的缓存，顾名思义就是对对象就行缓存，在本项目中，com.dapeng.seckill.service.SeckillUserService#getSeckillUserById和
     com.dapeng.seckill.service.SeckillUserService#getSeckillUserByToken的user对象都进行了缓存，但前者相当于数据库，用于登录时检验用户。
     而后者实际上是session。对于com.dapeng.seckill.service.SeckillUserService#updateSeckillUser方法中也缓存了对象，不过将更改的user先从redis中取出，
     然后删除对应的数据，然后在将新的数据更新到数据库中，再将数据缓存到redis中（getSeckillUserById）。
     
     为什么要先删除缓存在写入缓存呢？因为如果不删除，以前的请求仍然可以访问通过原来的token访问到以前的数据，除了造成数据的不一致还会有安全问题，
     所以需要删除以前的缓存在写入新的缓存，因为我们把getSeckillUserById缓存的user当数据库user用，必须保持一致性。
        更新缓存(token和user都要更新，先删除再添加)
           // 删除对象缓存（查询对象时再添加，好比实际中修改密码后重新登陆）
        redisService.delete(SeckillUserKeyPrefix.seckillUserKeyPrefixById,""+id);
          // 设置新的token缓存
        user.setPassword(toBeUpdateedUser.getPassword());
        redisService.set(SeckillUserKeyPrefix.token,token,user);// 为新的用户数据重新生成缓存（token不变只是覆盖掉了）
        
     如何保证redis和数据库数据一致性？Cache Aside Pattern！https://blog.csdn.net/diweikang/article/details/94406186
        更新的时候，先更新数据库，再删除缓存。
        读的时候，先读缓存；如果没有的话，就读数据库，同时将数据放入缓存，并返回响应。
        
        如果先删除缓存，然后再更新数据库：试想，两个并发操作，一个是更新操作，另一个是查询操作，更新操作删除缓存后，查询操作没有命中缓存，先把老数据读出来后放到缓存中，
        然后更新操作更新了数据库。于是，在缓存中的数据还是老的数据，导致缓存中的数据是脏的，而且还一直这样脏下去了。
      
     * 页面静态化、前后端分离(GET请求是幂等的，)
       页面静态化指的是将页面直接缓存到客户端。常用的技术有Angular.js，Vue.js。
       其实现方式就是通过ajax异步请求服务器获取动态数据，对于非动态数据部分缓存在客户端，客户端通过获取服务端返回的json数据解析完成相应的逻辑。
       在本项目中，我们对商品详情页和订单详情页做了一个静态化处理。对于商品详情页，异步地从服务端获取商品详情信息，然后客户端完成页面渲染工作
       除此之外，对于秒杀信息的获取也是通过异步获取完成的。例如，当秒杀开始时，用户执行秒杀动作，客户端就会轮询服务器获取秒杀结果。而不需要服务器直接返回页面。
       而对于订单详情页，实际上也是同样的思路。

10.静态资源优化

     * S/CSS压缩，减少流量。客户端完成解压工作。
     * 多个JS/CSS组合，减少连接数。一次TCP连接完成多个HTTP交互。
     * CDN就近访问。
     
     CDN的全称是Content Delivery Network，即内容分发网络。CDN是构建在网络之上的内容分发网络，依靠部署在各地的边缘服务器，通过中心平台的负载均衡、内容分发、调度等功能模块，
     使用户就近获取所需内容，降低网络拥塞，提高用户访问响应速度和命中率。CDN的关键技术主要有内容存储和分发技术。
     
     
11.秒杀接口的实现及其优化

    1.系统加载时执行afterPropertiesSet()，从数据库查出商品库存并存入redis，同时利用HashMap内存标记库存不为空。
    2.在SeckillController.doSeckill()方法中，首先查看内存标记，减少对redis的访问，若无库存直接结束，若秒杀未结束才继续访问redis
    3.利用decrStock = redisService.decr()预减库存，并根据利用decrStock更新内存标记。
    4.再判断是否已秒杀到，防止超卖。OrderService.createOrder()是把订单信息SeckillOrder同时插入了mysql和redis，但此处查的时候只从redis查。
          这里有个问题，在redis预减库存，入队，但实际队列中的秒杀可能失败，mysql还有库存时，而redis已经没有了，出现错误！
          比如队列里10个请求，两个是同一人的，肯定有一个请求失败，但此时redis中已经减去了这个，mysql并没有减去！
          所以，若已秒杀到，同过redisService.incr()回滚redis库存数据并更新内存标记。
    5.请求异步下单，将SeckillMessage信息（userId和goodsId）压入RabbitMQ，同时在MQListner监听队列，请求出队，取出message，再判断库存和是否已经秒杀到；如果通过，再
    OrderInfo = seckillService.seckill()，减库存(Boolean success = goodsService.reduceStock(goodsVo))和下订单(orderService.createOrder(user,goodsVo))。
    6.返回前端：排队中。客户端轮询（轮询从Redis里查createOder里存进去的订单信息，同时兼顾Boolean isOver = getGoodsOver(goodsId)以判断是卖完了没订单，还是没卖完只是在排
    队，还是秒杀成功），及时返回信息，以增强用户体验。
    
12.超卖问题，Redis和数据库数据一致性问问题

    1.MySQL商品库存减为负值。
    在SeckillService中：
      // 减库存-下订单-写入秒杀订单(原子操作，事务)
      @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
      public OrderInfo seckill(SeckillUser user, GoodsVo goodsVo) {
      
          //减库存
          Boolean success = goodsService.reduceStock(goodsVo);
          if (success){
          
              //下订单（事务）-将orderInfo插入MySQL，seckillOrder同时插入到MySQL和Redis
              return orderService.createOrder(user,goodsVo);
              
          }else {
          
              //redis中标记，是否卖完
              setGoodsOver(goodsVo.getId());
              return null;
          }
      }
      
     在OrderService中：
      @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public OrderInfo createOrder(SeckillUser user, GoodsVo goodsVo) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goodsVo.getId());
        orderInfo.setGoodsName(goodsVo.getGoodsName());
        orderInfo.setGoodsPrice(goodsVo.getSeckillPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());

        // 将订单信息插入order_info表中
        Long orderId = orderDao.insertOrderInfo(orderInfo);

        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setGoodsId(goodsVo.getId());
        // seckillOrder.setOrderId(orderId);
        seckillOrder.setOrderId(orderInfo.getId());
        seckillOrder.setUserId(user.getId());

        // 将秒杀订单插入seckill_order表中
        orderDao.insertSeckillOrder(seckillOrder);
        // 将秒杀订单存入redis缓存中
        redisService.set(OrderKeyPrefix.getSeckillOrderByUidGid, "" + user.getId() + goodsVo.getId(), seckillOrder);

        return orderInfo;
    }
      
    解决：因为每次UPDATE对于数据库来说都是原子的，如果每次减库存操作之前先判断库存是否大于零，则可以利用数据库层面的原子性来保证库存不会为负数。
    //mysql本身会对这条记录加个锁，不会出现两个线程同时更新同一条记录的情况（行锁）
    @Update("update seckill_goods set stock_count = stock_count-1 where goods_id = #{goodsId} and stock_count>0")
    Integer reduceStock(SeckillGoods seckillGoods);
    
    2.同一个用户秒杀到了两个相同的产品。
      如果一个未秒杀成功的用户同时对一个商品发出两次秒杀请求，对于两次秒杀请求，服务器层面会判断用户的两次秒杀请求为合法请求，然后完成从数据库减库存和将订单插入到数据库的操
      作，显然，这是不合理的。因为一个用户只能秒杀一个商品，如果执行成功，则订单表中会出现两条条商品id和用户id相同的记录，一个商品的库存被同一个用户减了两次（也可能是多
      次），这就引发了超卖问题。
      
      因此，为了解决这个问题，我们要充分利用事务的特性。 从数据库减库存和将订单记录插入到数据库(和redis，在OrderService里orderDao.insertSeckillOrder(seckillOrder)实现)
      构成了事务，如果一个操作未执行成功，则事务会回滚。如果我们对seckill_order中的user_id和goods_id字段创建一个联合唯一索引，则在插入两条user_id和goods_id相同的记录时，
      将会操作失败，从而事务回滚，秒杀不成功，这就解决了同一个用户发起对一个商品同时发起多次请求引发的超卖问题。
      同时意识到，请求出队时才下单，先查询MySQL库存，再从redis里判断是否已经秒杀到（因为redis里的订单信息是在createOrder成功后才插入的，到这一步时已经无可能重复下单了，唯
      一索引已经解决），然后seckillService.seckill里减库存，下订单，订单记录插入redis和数据库。

13.秒杀接口隐藏+数学公式验证码

    在秒杀开始之前，秒杀接口地址不要写到客户端，而是在秒杀开始之后，将秒杀地址动态地在客户端和服务器间进行交互完成拼接。这样一来，秒杀开始之前，秒杀地址对客户端不可见。
    实现思路：
      1.秒杀开始之前，先去请求接口获取秒杀地址；
      2.接口改造，带上@pathVariable参数；
      3.添加生成地址的接口；
      4.秒杀收到请求，先验证@pathVariable参数。
      
    用户在提交获取秒杀地址的请求之前，将需要将goodsId和verifyCode一同提交到服务端，服务器getSeckillPath()通过@RequestParam参数校验验证码，通过则返回path盐值，
    然后前端持此"/seckill/" + path盐值 + "/doSeckill"拼接秒杀地址去异步地请求秒杀，服务器再验证path盐值是否正确，正确则就秒杀，此时客户端轮询等待结果返回。这样就完成了秒杀接口地址的隐藏。
    需要注意的是，这里需要将goodsId和verifyCode一同提交到服务端做校验，如果只提交goodsId，那么客户端仍然可以使用明文的方式获取随机生成的接口秒杀地址，
    但是，引入了verifyCode后，客户端需要将验证码也一起发送到服务端做验证，验证成功才返回随机生成的秒杀地址，不成功则返回非法请求。
    通过这样一种双重验证的方式，就可以方式用户使用不合理的手段参与秒杀，引入验证码有效地防止了这一点，因为验证码的输入需要用户真正参与进来。
    
14.数学公式验证码

    验证码的作用：
      1.防止利用机器人等手段防止非目标用户参与秒杀；
      2.减少单位时间内的请求数量。对于一个秒杀商品，在开始秒杀后肯定会有许多用户参与秒杀，那么在开始秒杀的时候，用户请求数量是巨大，从而对服务器产生较大的压力，而通过验证码的方式就可以有效地将集中式的请求分散，从而达到削减请求峰值的目的。
    实现思路：
      在服务端计算出验证码的表达式的值，存储在服务端，客户端输入验证码的表达式值，传入服务端进行验证。
        1.点击秒杀之前，向让用户输入验证码，分散用户的请求；
        2.添加生成验证码的接口；
        3.在获取秒杀路径的时候，验证验证码；
        4.ScriptEngine的使用（用于计算验证码上的表达式）。
        
 15.接口限流防刷
 
     使用拦截器实现，当然也可以用计时器或者Guava RateLimiter实现。
     自定义@AccessLimiter注解，设定：时间、次数、是否需要登录。
     在redis中存储一个用于记录访问次数的变量，在过期时间内被继续访问，则次数变量加1，如果在过期时间内访问次数超出限制，则返回“频繁提交提示用户”。过期时间到了之后，将该变量删除。
     在AccessLimitHandlerInterceptor里拦截标注了@AccessLimit注解的方法，取出注解参数，key为请求URI+userId，然后从redis里get请求次数，若无数据则set。否则对比次数并incr redis数据，并返回前端提示信息。
     
     因为可能需要对很对接口对限流防刷操作，如果对每一个接口都实现一遍限流防刷，则会导致代码过度冗余，因此，可以定义一个方法拦截器@AccessInterceptor拦截用户对接口的请求，
     统一对拦截限流逻辑处理，这样可以有效地减少代码的冗余。针对需要拦截请求的接口，添加注解@AccessLimit即可。
     
    //在在AccessLimitHandlerInterceptor里@Autowired userService为空，造成null的原因是因为拦截器加载是在SpringContext创建之前完成的，所以在拦截器中注入实体自然就为null。
    解决方式：在注册Interceptor的配置类里就@Bean添加拦截器到IoC容器，不要在addArgumentResolvers里new拦截器，直接注入就好。
    @Bean
    public AccessLimitHandlerInterceptor getInterceptor(){
        return new AccessLimitHandlerInterceptor();
    }
     
      
    
     
     

  
  
